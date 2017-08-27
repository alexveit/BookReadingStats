package com.veit.alex.sudokusolver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'sudoku_puzzle' library on application startup.
    static {
        //System.loadLibrary("native-lib");
        System.loadLibrary("sudoku_puzzle");
    }

    private class SolveTask extends AsyncTask<Void, Void, Void> {

        String mResult;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adjustButtonLayoutUI(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            //String test = "000020409208004000045007062800203570604000903037608004450100320000300701301080000";
            mResult = getSolvedPuzzleString(MainActivity.this.getInputString());
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
            adjustButtonLayoutUI(false);
            if (mResult.contains("#")) {
                Toast.makeText(MainActivity.this, mResult.substring(1), Toast.LENGTH_LONG).show();
                return;
            }
            MainActivity.this.setInputFromString(mResult);
        }

    }

    private class GetPuzzleFromWebTask extends AsyncTask<Void, Void, Void> {

        StringBuilder mPuzzleFromWeb;
        IOException mIOE;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adjustButtonLayoutUI(true);
            mPuzzleFromWeb = new StringBuilder();
            mIOE = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Document doc = null;
            try {
                doc = Jsoup.connect("http://view.websudoku.com/?" + mDifficultyString).get();
            } catch (IOException ioe) {
                mIOE = ioe;
            }

            if(doc != null) {
                Elements trs = doc
                        .getElementById("puzzle_grid")
                        .getElementsByTag("tr");

                for (Element tr : trs) {
                    Elements tds = tr.getElementsByTag("td");
                    for (Element td : tds) {
                        Elements inputs = td.getElementsByTag("input");
                        for (Element input : inputs) {
                            String val = input.attributes().get("VALUE");
                            if (!val.matches(""))
                                mPuzzleFromWeb.append(val);
                            else
                                mPuzzleFromWeb.append("0");
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
            adjustButtonLayoutUI(false);
            if(mIOE == null) {
                identifyLockedCells(mPuzzleFromWeb.toString());
                setInputFromString(mPuzzleFromWeb.toString());
            }
            else
                Toast.makeText(MainActivity.this,
                        "Make sure internet is connected...", Toast.LENGTH_LONG).show();
        }

    }

    private class DifficultyView {

        TextView mText;
        Animation mAnim;

        DifficultyView(MainActivity mainActivity, TextView text) {
            mText = text;
            mAnim = AnimationUtils.loadAnimation(mainActivity, R.anim.fade_in_slow);
            mAnim.setInterpolator(new MyBounceInterpolator(0.2, 20));
        }

        void startAnimation() { mText.startAnimation(mAnim); }

        void setTextSize(float size) { mText.setTextSize(TypedValue.COMPLEX_UNIT_PX,size); }

        void setEnabled(boolean enabled) { mText.setEnabled(enabled); }
    }

    private static final String PREF_STR = "PREF";

    private static final String INPUT_STR = "INPUT";

    private static final String LOCKED_STR = "LOCKED_STR";

    private static final String DIFF_STR = "DIFFSTR";
    private static final String EASY_STR = "level=1";
    private static final String MEDIUM_STR = "level=2";
    private static final String HARD_STR = "level=3";
    private static final String EVIL_STR = "level=4";

    private static final String DIFF_ID = "DIFFID";
    private static final int EASY_ID = 0;
    private static final int MEDIUM_ID = 1;
    private static final int HARD_ID = 2;
    private static final int EVIL_ID = 3;

    private static final int NORMAL_COLOR = android.R.color.tertiary_text_dark;
    private static final int EASY_COLOR = android.R.color.holo_green_dark;
    private static final int MEDIUM_COLOR = android.R.color.holo_blue_dark;
    private static final int HARD_COLOR = android.R.color.holo_red_light;
    private static final int EVIL_COLOR = android.R.color.holo_red_dark;

    private static final int[] CELL_ID = {
            R.id.r0c0,R.id.r0c1,R.id.r0c2,
            R.id.r0c3,R.id.r0c4,R.id.r0c5,
            R.id.r0c6,R.id.r0c7,R.id.r0c8,

            R.id.r1c0,R.id.r1c1,R.id.r1c2,
            R.id.r1c3,R.id.r1c4,R.id.r1c5,
            R.id.r1c6,R.id.r1c7,R.id.r1c8,

            R.id.r2c0,R.id.r2c1,R.id.r2c2,
            R.id.r2c3,R.id.r2c4,R.id.r2c5,
            R.id.r2c6,R.id.r2c7,R.id.r2c8,

            R.id.r3c0,R.id.r3c1,R.id.r3c2,
            R.id.r3c3,R.id.r3c4,R.id.r3c5,
            R.id.r3c6,R.id.r3c7,R.id.r3c8,

            R.id.r4c0,R.id.r4c1,R.id.r4c2,
            R.id.r4c3,R.id.r4c4,R.id.r4c5,
            R.id.r4c6,R.id.r4c7,R.id.r4c8,

            R.id.r5c0,R.id.r5c1,R.id.r5c2,
            R.id.r5c3,R.id.r5c4,R.id.r5c5,
            R.id.r5c6,R.id.r5c7,R.id.r5c8,

            R.id.r6c0,R.id.r6c1,R.id.r6c2,
            R.id.r6c3,R.id.r6c4,R.id.r6c5,
            R.id.r6c6,R.id.r6c7,R.id.r6c8,

            R.id.r7c0,R.id.r7c1,R.id.r7c2,
            R.id.r7c3,R.id.r7c4,R.id.r7c5,
            R.id.r7c6,R.id.r7c7,R.id.r7c8,

            R.id.r8c0,R.id.r8c1,R.id.r8c2,
            R.id.r8c3,R.id.r8c4,R.id.r8c5,
            R.id.r8c6,R.id.r8c7,R.id.r8c8,
    };

    private static final int[] DIFFICULTY_ID = {
            R.id.tEasy,R.id.tMedium,
            R.id.tHard,R.id.tEvil,
    };

    private TextView mLastCellSelected;
    private ArrayList<TextView> mCell;
    private ArrayList<DifficultyView> mDifficulty;
    private ProgressBar mProgressBar;
    private LinearLayout mButtonLayout;
    private String mDifficultyString;
    private int mDifficultyID;
    private ArrayList<Integer> mLockedCell;
    private int mLockedColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonLayout = (LinearLayout) findViewById(R.id.buttonLayout);

        mDifficulty = new ArrayList<>();
        for(int diffIndex = 0; diffIndex < DIFFICULTY_ID.length; diffIndex++) {
            mDifficulty.add(new DifficultyView(this,
                    (TextView)findViewById(DIFFICULTY_ID[diffIndex])));
        }

        mCell = new ArrayList<>();
        for(int boxNum = 0; boxNum < CELL_ID.length; boxNum++) {
            mCell.add((TextView) findViewById(CELL_ID[boxNum]));
            mCell.get(boxNum).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mProgressBar.getVisibility() != View.VISIBLE) {
                        if(mLastCellSelected != null)
                            mLastCellSelected.setBackground(getDrawable(R.drawable.black_box));
                        view.setBackground(getDrawable(R.drawable.yellow_box));
                        mLastCellSelected = (TextView) view;
                    }
                }
            });
        }

        final TableRow tRow = (TableRow) findViewById(R.id.rowWidth);

        ViewTreeObserver vto = tRow.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int cellSize  = tRow.getMeasuredWidth()/9;
                for(int cellNum = 0; cellNum < CELL_ID.length; cellNum++) {
                    mCell.get(cellNum).setHeight(cellSize);
                    mCell.get(cellNum).setWidth(cellSize);
                }
            }
        });

        mLockedCell = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences(PREF_STR, Context.MODE_PRIVATE);
        mDifficultyString = prefs.getString(DIFF_STR, EASY_STR);
        mDifficultyID = prefs.getInt(DIFF_ID, EASY_ID);

        String lockedCells = prefs.getString(LOCKED_STR, "");
        StringTokenizer st = new StringTokenizer(lockedCells, ",");
        int lockedCellCount = st.countTokens();
        for (int i = 0; i < lockedCellCount; i++) {
            mLockedCell.add(Integer.parseInt(st.nextToken()));
        }

        identifyLockedColor();
        adjustDifficultyUI();
        setInputFromString(prefs.getString(INPUT_STR, ""));
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences(PREF_STR, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(INPUT_STR, getInputString());
        editor.putString(DIFF_STR, mDifficultyString);
        editor.putInt(DIFF_ID, mDifficultyID);

        StringBuilder sbLockedCells = new StringBuilder();
        for (int i = 0; i < mLockedCell.size(); i++)
            sbLockedCells.append(mLockedCell.get(i)).append(",");
        editor.putString(LOCKED_STR, sbLockedCells.toString());

        editor.apply();
    }

    public void numClick(View v) {
        if(mLastCellSelected != null) {
            String numString = "";
            switch(v.getId()) {
                case R.id.b1: numString = "1"; break;
                case R.id.b2: numString = "2"; break;
                case R.id.b3: numString = "3"; break;
                case R.id.b4: numString = "4"; break;
                case R.id.b5: numString = "5"; break;
                case R.id.b6: numString = "6"; break;
                case R.id.b7: numString = "7"; break;
                case R.id.b8: numString = "8"; break;
                case R.id.b9: numString = "9"; break;
            }
            mLastCellSelected.setText(numString);
        }
    }

    public void funcClick(View v) {
        switch(v.getId()) {
            case R.id.bClearCell:
                if(mLastCellSelected != null)
                    mLastCellSelected.setText("");
                return;
            case R.id.bClearGrid: clearGrid(); return;
            case R.id.bSolve:
                if(isEmptyPuzzle()) {
                    Toast.makeText(this, "Empty Puzzle...", Toast.LENGTH_LONG).show();
                    return;
                }
                clearCellSelection();
                new SolveTask().execute();
                return;
        }
    }

    public void difficultyClick(View v) {
        switch(v.getId()) {
            case R.id.tEasy:
                mDifficultyString = EASY_STR;
                mDifficultyID = EASY_ID;
                break;
            case R.id.tMedium:
                mDifficultyString = MEDIUM_STR;
                mDifficultyID = MEDIUM_ID;
                break;
            case R.id.tHard:
                mDifficultyString = HARD_STR;
                mDifficultyID = HARD_ID;
                break;
            case R.id.tEvil:
                mDifficultyString = EVIL_STR;
                mDifficultyID = EVIL_ID;
                break;
        }
        adjustDifficultyUI();
        clearGrid();
        identifyLockedColor();
        new GetPuzzleFromWebTask().execute();
    }

    private void adjustDifficultyUI() {
        for(int diffI = 0; diffI < DIFFICULTY_ID.length; diffI++) {
            if(diffI == mDifficultyID) {
                mDifficulty.get(diffI).setTextSize(getResources().getDimension(R.dimen.largeSize));
                mDifficulty.get(diffI).startAnimation();
            } else {
                mDifficulty.get(diffI).setTextSize(getResources().getDimension(R.dimen.normalSize));
            }
        }
    }

    private void adjustButtonLayoutUI(boolean isAsyncTaskRunning) {
        if(isAsyncTaskRunning) {
            mProgressBar.setVisibility(View.VISIBLE);
            mButtonLayout.setVisibility(View.GONE);
            for(int diffI = 0; diffI < DIFFICULTY_ID.length; diffI++) {
                mDifficulty.get(diffI).setEnabled(false);
            }
        } else {
            mProgressBar.setVisibility(View.GONE);
            mButtonLayout.setVisibility(View.VISIBLE);
            for(int diffI = 0; diffI < DIFFICULTY_ID.length; diffI++) {
                mDifficulty.get(diffI).setEnabled(true);
            }
        }
    }

    private void clearCellSelection() {
        if(mLastCellSelected != null) {
            mLastCellSelected.setBackground(getDrawable(R.drawable.black_box));
            mLastCellSelected = null;
        }
    }

    private void clearGrid() {
        for(int cellNum = 0; cellNum < CELL_ID.length; cellNum++) {
            mCell.get(cellNum).setText("");
            mCell.get(cellNum).setTextColor(getResources().getColor(NORMAL_COLOR,null));
            mCell.get(cellNum).setClickable(true);
        }
        mLockedCell.clear();
        clearCellSelection();
    }

    private boolean isEmptyPuzzle() {
        for(int cellNum = 0; cellNum < mCell.size(); cellNum++){
            if(mCell.get(cellNum).getText().toString() != "")
                return false;
        }
        return true;
    }

    private String getInputString() {
        StringBuilder puzzleInput = new StringBuilder();
        for(int cellNum = 0; cellNum < mCell.size(); cellNum++){
            if(mCell.get(cellNum).getText().toString().matches(""))
                puzzleInput.append("0");
            else
                puzzleInput.append(mCell.get(cellNum).getText());
        }
        return puzzleInput.toString();
    }

    private void identifyLockedCells(String result) {
        if(result.length() == mCell.size()) {
            char ch;
            for (int cell = 0; cell < mCell.size(); cell++) {
                ch = result.charAt(cell);
                if(ch != '0')
                    mLockedCell.add(cell);
            }
        }
    }

    private void identifyLockedColor() {
        switch(mDifficultyID) {
            case EASY_ID: mLockedColor = EASY_COLOR; break;
            case MEDIUM_ID: mLockedColor = MEDIUM_COLOR; break;
            case HARD_ID: mLockedColor = HARD_COLOR; break;
            case EVIL_ID: mLockedColor = EVIL_COLOR; break;
        }
    }

    private void setInputFromString(String result) {
        if(result.length() == mCell.size()) {
            char ch;
            for (int cellNum = 0; cellNum < mCell.size(); cellNum++) {
                ch = result.charAt(cellNum);
                if(ch != '0')
                    mCell.get(cellNum).setText(String.valueOf(ch));
                else
                    mCell.get(cellNum).setText("");

                if(mLockedCell.contains(cellNum)) {
                    mCell.get(cellNum).setTextColor(getResources().getColor(mLockedColor,null));
                    mCell.get(cellNum).setClickable(false);
                }
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //private native String stringFromJNI();

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private static native String getSolvedPuzzleString(String inputFromPuzzle);
}
