package com.veit.alex.est;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.veit.alex.est.util.MySpeakListAdapter;
import com.veit.alex.est.util.Utils;

public class SpeakListActivity extends AppCompatActivity {

    private MySpeakListAdapter mMsla = null;
    private boolean mNeedsToWriteFile = false;
    private boolean mFromStoryList = false;
    private int mBookNum = -1;
    private int mStoryNum = -1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(mMsla.processActivityResult(requestCode,resultCode,data))
            return;

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak);

        mBookNum = getIntent().getExtras().getInt(Utils.BOOK_NUM);
        mStoryNum = getIntent().getExtras().getInt(Utils.STORY_NUM);
        mFromStoryList = getIntent().getExtras().getBoolean(Utils.STORY_LIST);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle(Utils.getStoryTitleString(mBookNum, mStoryNum));

        mNeedsToWriteFile = false;

        mMsla = new MySpeakListAdapter(this, (TextView) findViewById(R.id.textViewElapsed),mBookNum, mStoryNum);

        ((ListView) findViewById(R.id.speakListView)).setAdapter(mMsla);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        if(mFromStoryList)
            getMenuInflater().inflate(R.menu.activity_listen, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home: onBackPressed(); return true;

            case R.id.menuListen:

                Intent intent = new Intent(this, StoryActivity.class);
                intent.putExtra(Utils.BOOK_NUM, mBookNum);
                intent.putExtra(Utils.STORY_NUM, mStoryNum);
                startActivity(intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mNeedsToWriteFile) {
            Utils.writeToFile(this);
        }
    }

    public void needsToWriteFile() { mNeedsToWriteFile = true; }
}
