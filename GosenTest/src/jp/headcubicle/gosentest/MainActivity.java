package jp.headcubicle.gosentest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.reduls.sanmoku.Morpheme;
import net.reduls.sanmoku.Tagger;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {

    private TestSQLiteOpenHelper helper = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        helper = new TestSQLiteOpenHelper(getApplicationContext(), "words.db", null, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 学習.
     */
    public void onClickLearning(View view) {
        // DBを開く
        SQLiteDatabase db = helper.getWritableDatabase();
        
        // Tweetファイル読み込み
        AssetManager assetManager = this.getResources().getAssets();
        
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        
        try {
            try {
                inputStream = assetManager.open("tweets/2013_03.js");
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String str = null;
                while ((str = bufferedReader.readLine()) != null) {
                    if(!str.startsWith("Grailbird")) {
                        stringBuilder.append(str);                        
                    }
                }
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        // 語句をinsertする。
//        StringTagger tagger = SenFactory.getStringTagger(null);
//        List<Token> tokens = new ArrayList<Token>();
        
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(stringBuilder.toString());
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject text = jsonArray.getJSONObject(i);
                String tweet = text.getString("text");
                
                // RTを削除
                if (tweet.startsWith("RT ")) {
                    tweet = tweet.substring(tweet.indexOf(" "));
                }
                
                // ユーザIDを削除
                tweet = tweet.replaceAll("@\\w+:?", "");
                
                Log.d("test", tweet);
                
                List<Morpheme> morphemeList = Tagger.parse(tweet);
//                tagger.analyze(tweet, tokens);

//                for (int j = 0; (j + 2) < tokens.size(); j++) {
//                    ContentValues values = new ContentValues();
//                    int type = 1;
//                    
//                    // 0: 先頭、1: 中間、2: 末尾
//                    if(j == 0) {
//                        type = 0;
//                    } else if ((j + 2) == (tokens.size() - 1)) {
//                        type = 2;
//                    }
//                    
//                    values.put("type", type);
//                    values.put("first", tokens.get(j).getSurface());
//                    values.put("second", tokens.get(j + 1).getSurface());
//                    values.put("third", tokens.get(j + 2).getSurface());
//                    
//                    db.insert("words", null, values);
//                }        

                for (int j = 0; (j + 2) < morphemeList.size(); j++) {
                    ContentValues values = new ContentValues();
                    int type = 1;
                    
                    // 0: 先頭、1: 中間、2: 末尾
                    if(j == 0) {
                        type = 0;
                    } else if ((j + 2) == (morphemeList.size() - 1)) {
                        type = 2;
                    }
                    
                    values.put("type", type);
                    values.put("first", morphemeList.get(j).surface);
                    values.put("second", morphemeList.get(j + 1).surface);
                    values.put("third", morphemeList.get(j + 2).surface);
                    
                    db.insert("words", null, values);
                }        
            }
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
        }
        
        // 検索テスト
        Cursor cursor = db.query("words",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);
        
        if (cursor.moveToFirst()) {
            do {
                Log.d("test", "type: " + cursor.getInt(cursor.getColumnIndex("type")) + ", "
                            + "first: " + cursor.getString(cursor.getColumnIndex("first")) + ", "
                            + "second: " + cursor.getString(cursor.getColumnIndex("second")) + ", "
                            + "third: " + cursor.getString(cursor.getColumnIndex("third"))
                            );
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
    }

    /**
     * Tweet.
     */
    public void onClickTweet(View view) {
        // DBを開く
        SQLiteDatabase db = helper.getReadableDatabase();

        ArrayList<Word> wordList = new ArrayList<Word>();
        
        // 先頭を検索
        Cursor cursor = null;
        int rowCount = 0;
        Random random = null;
        try {
            cursor = db.query("words",
                                null,
                                "type=0",
                                null,
                                null,
                                null,
                                null);
            
            rowCount = cursor.getCount();
            random = new Random();
            
            if (rowCount > 0) {
                cursor.moveToPosition(random.nextInt(rowCount));
    
                Word head = new Word();
                head.setType(cursor.getInt(cursor.getColumnIndex("type")));
                head.setFirst(cursor.getString(cursor.getColumnIndex("first")));
                head.setSecond(cursor.getString(cursor.getColumnIndex("second")));
                head.setThird(cursor.getString(cursor.getColumnIndex("third")));
                
                wordList.add(head);
                Log.d("test", "type: " + cursor.getInt(cursor.getColumnIndex("type")) + ", "
                        + "first: " + cursor.getString(cursor.getColumnIndex("first")) + ", "
                        + "second: " + cursor.getString(cursor.getColumnIndex("second")) + ", "
                        + "third: " + cursor.getString(cursor.getColumnIndex("third"))
                        );
            }
        } finally {
            cursor.close();
        }
        
        // 中間を検索
        while (wordList.get(wordList.size() - 1).getType() != 2) {
            Word prev = wordList.get(wordList.size() - 1);
            
            try {
                cursor = db.query("words",
                                null,
                                "first=? and second=?",
                                new String[] {prev.getSecond(), prev.getThird()},
                                null,
                                null,
                                null);    
        
                rowCount = cursor.getCount();
                if (rowCount > 0) {
                    cursor.moveToPosition(random.nextInt(rowCount));
    
                    Word middle = new Word();
                    middle.setType(cursor.getInt(cursor.getColumnIndex("type")));
                    middle.setFirst(cursor.getString(cursor.getColumnIndex("first")));
                    middle.setSecond(cursor.getString(cursor.getColumnIndex("second")));
                    middle.setThird(cursor.getString(cursor.getColumnIndex("third")));
                        
                    wordList.add(middle);
                    Log.d("test", "type: " + cursor.getInt(cursor.getColumnIndex("type")) + ", "
                            + "first: " + cursor.getString(cursor.getColumnIndex("first")) + ", "
                            + "second: " + cursor.getString(cursor.getColumnIndex("second")) + ", "
                            + "third: " + cursor.getString(cursor.getColumnIndex("third"))
                            );
                }
            } finally {
                cursor.close();
            }
        }

        
        // 文章を生成する。
        String text = "";
        for(int i = 0; i < wordList.size(); i++) {
            Word word = wordList.get(i);
            if(i == 0) {
                text = word.getFirst() + word.getSecond() + word.getThird();
            } else {
                text += word.getThird();
            }
        }

        Log.d("test", text);
        
        wordList.clear();
        db.close();
    }
    
    private static class TestSQLiteOpenHelper extends SQLiteOpenHelper {

        private String createSql = "CREATE TABLE words (_id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL, first TEXT, second TEXT, third TEXT);";
        
        public TestSQLiteOpenHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(createSql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
        }
        
    }    
}
