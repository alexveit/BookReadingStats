package com.veit.alex.est;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class StoryListActivity extends AppCompatActivity {

    private StoryListAdapter mMsla = null;

    private int mBookNum = -1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMsla.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storylist);

        mBookNum = getIntent().getExtras().getInt(Utils.BOOK_NUM);

        int bookNumForDisplay = mBookNum + 1;

        getSupportActionBar().setTitle("Book " + bookNumForDisplay + " - Stories");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView storyListView = (ListView) findViewById(R.id.storyListView);

        mMsla = new StoryListAdapter(this, mBookNum);

        storyListView.setAdapter(mMsla);

        storyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent,
                                    final View view, final int storyNum, final long id) {

                Intent intent = new Intent(StoryListActivity.this, StoryActivity.class);
                intent.putExtra(Utils.BOOK_NUM, mBookNum);
                intent.putExtra(Utils.STORY_NUM, storyNum);
                startActivityForResult(intent, 0);

            }
        });

        storyListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int storyNum, long id) {

                Intent intent = new Intent(StoryListActivity.this, SpeakListActivity.class);
                intent.putExtra(Utils.BOOK_NUM, mBookNum);
                intent.putExtra(Utils.STORY_NUM, storyNum);
                intent.putExtra(Utils.STORY_LIST, true);
                startActivityForResult(intent, 0);

                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: onBackPressed(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
