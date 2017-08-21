package alex.veit.bookreadingstats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class ChronActivity extends Activity {

    private Button _buttonStartChron;
    private Button _buttonStopChron;
    private Button _buttonResetChron;
    private Button _buttonDoneChron;
    private Button _buttonCancelChron;

    private EditText _page;

    private boolean _chronHasStarted;
    private boolean _chronIsRunning;
    private boolean _updateMillisec;
    private long _base;
    private long _elapsed;
    private long _myMilisec;
    private long _transferMillisec;
    private BookStats _bs;
    private Calendar _start;
    private Calendar _end;
    private Chronometer _chronometer;
    private NotificationCompat.Builder _builder;
    private NotificationManager _notificationManager;

    protected boolean autoAddSession() {
        long elapsedMillis = _elapsed - _chronometer.getBase();
        elapsedMillis /= 1000;
        if (elapsedMillis < 0) {
            elapsedMillis = _myMilisec / 1000;
        }
        if (_bs.isValidSession(elapsedMillis)) {
            _transferMillisec = elapsedMillis;
            //setPageDialog();
            return true;
        } else {
            new AlertDialog.Builder(this).setTitle(getString(R.string.alert))
                    .setMessage("Please read for more then 1 minute.")
                    .setPositiveButton("OK", null).show();
            return false;
        }
    }

    private void chronStart(final boolean display) {
        _chronometer.setBase(SystemClock.elapsedRealtime() + _base);
        _chronometer.start();
        _buttonStartChron.setEnabled(false);
        _buttonStopChron.setEnabled(true);
        _buttonDoneChron.setEnabled(false);
        _buttonResetChron.setEnabled(false);
        _buttonCancelChron.setEnabled(false);
        _page.setEnabled(false);
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
        _buttonCancelChron.setEnabled(true);
        _page.setEnabled(true);
        _chronIsRunning = false;
        _builder.setContentText("Stopped");
        _notificationManager.notify(0, _builder.build());
        _end = Calendar.getInstance();
    }

    private void chronReset() {
        _chronometer.setBase(SystemClock.elapsedRealtime());
        _base = 0;
        _chronIsRunning = false;
        _chronHasStarted = false;
        _buttonResetChron.setEnabled(false);
        _buttonDoneChron.setEnabled(false);
        _buttonCancelChron.setEnabled(true);
        _page.setEnabled(false);

    }

    private void chronDone() {
        _chronometer.setBase(SystemClock.elapsedRealtime());
        _base = 0;
        _notificationManager.cancel(0);

        _bs._alertNewSessions = true;
        final Intent returnIntent = new Intent(this, ChronActivity.class);
        returnIntent.putExtra(BookStats._CLASS, _bs);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void chronCancel() {
        _chronometer.stop();
        _chronometer.setBase(SystemClock.elapsedRealtime());
        _base = 0;
        _notificationManager.cancel(0);
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chron);

        _bs = (BookStats) getIntent().getParcelableExtra(BookStats._CLASS);
        _chronometer = (Chronometer) findViewById(R.id.chronometer);

        _buttonCancelChron = (Button) findViewById(R.id.buttonCancel);
        _buttonStartChron = (Button) findViewById(R.id.buttonStart);
        _buttonStopChron = (Button) findViewById(R.id.buttonStop);
        _buttonResetChron = (Button) findViewById(R.id.buttonReset);
       // _buttonDoneChron = (Button) findViewById(R.id.buttonDone);

        _buttonDoneChron = (Button) findViewById(R.id.buttonDone);
        _page = (EditText) findViewById(R.id.editTextPage);

        _start = Calendar.getInstance();

        _buttonDoneChron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if(autoAddSession())
                {
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
                        } else {
                            new AlertDialog.Builder(ChronActivity.this)
                                    .setTitle(getString(R.string.alert))
                                    .setMessage(
                                            "Pages read can not be less then "
                                                    + _bs.getPagesReadInt())
                                    .setPositiveButton("OK", null).show();
                            _page.setText(_bs.getPagesReadString());
                        }

                    } else {
                        new AlertDialog.Builder(ChronActivity.this)
                                .setTitle(getString(R.string.alert))
                                .setMessage(
                                        "Pages read can not be grater then total pages.\nTotal pages: "
                                                + _bs._totalPages)
                                .setPositiveButton("OK", null).show();
                        _page.setText(_bs.getPagesReadString());
                    }
                }
            }
        });

        final TextView total = (TextView) findViewById(R.id.textViewTPages);

        total.setText(getResources().getString(R.string.totalPages) + " "
                + _bs._totalPages);

        _page.setText(_bs.getPagesReadString());

        _buttonStartChron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                chronStart(true);
            }

        });

        _buttonStopChron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                chronStop();
            }

        });

        _buttonResetChron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new AlertDialog.Builder(ChronActivity.this)
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

        _buttonCancelChron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (_chronHasStarted) {
                    new AlertDialog.Builder(ChronActivity.this)
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



        _builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_book).setContentTitle(_bs._name)
                .setContentText("Stopped").setOngoing(true);
        final Intent contentIntent = new Intent(this, ChronActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        _builder.setContentIntent(PendingIntent.getActivity(
                this.getBaseContext(), 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT));
        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        PhoneStateListener psl = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        // Here you can perform your task while phone is ringing.
                        if(_chronIsRunning)
                            chronStop();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        break;
                }
            }

        };

        TelephonyManager  manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        manager.listen(psl, PhoneStateListener.LISTEN_CALL_STATE); // Registers a listener object to receive notification of changes in specified telephony states.



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_chron, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        switch(id)
        {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                //onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
