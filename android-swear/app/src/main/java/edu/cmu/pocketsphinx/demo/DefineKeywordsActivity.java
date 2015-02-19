package edu.cmu.pocketsphinx.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;


public class DefineKeywordsActivity extends Activity {
    private ArrayList<HashMap<String, String>> input_list;
    private EditText input_text;
    private ListView lv;
    private ArrayList<String> dictionaryList;
    private ArrayList<String> presetWords;
    Button enterButton;
    SimpleAdapter simpleAdapter;
    Spinner presetWordsDropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up notitle
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //set up full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_define_keywords);
        input_list = new ArrayList<HashMap<String, String>>();
        input_text = (EditText) findViewById(R.id.editText);
        enterButton = (Button) findViewById(R.id.button);
        input_text.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    enterButton.performClick();
                    return true;
                }
                return false;
            }
        });

        createDictionary();

        lv = (ListView) findViewById(R.id.listView);
        simpleAdapter = new SimpleAdapter(this,
                input_list,
                R.layout.forlistview,
                new String[] {"word", "dumb"}, new int[] {R.id.textView, R.id.button2});

        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.button2)
                {
                    ImageButton b=(ImageButton) view;
                    b.setOnClickListener( new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            String textViewValue = "";
                            for(int i=0; i<((ViewGroup)v.getParent()).getChildCount(); ++i) {
                                View nextChild = ((ViewGroup) v.getParent()).getChildAt(i);
                                if (nextChild.getId() == R.id.textView) {
                                    textViewValue = ((TextView) nextChild).getText().toString();
                                }
                            }

                            ArrayList<HashMap<String, String>> newInputList = new ArrayList<HashMap<String, String>>();
                            for (int i = 0; i < input_list.size(); i++) {
                                if (!input_list.get(i).get("word").equals(textViewValue)) {
                                    Log.d("newInputList", input_list.get(i).get("word"));
                                    newInputList.add(input_list.get(i));
                                }
                            }
                            input_list.clear();
                            input_list.addAll(newInputList);
                            simpleAdapter.notifyDataSetChanged();
                        }
                    });
                    return true;
                }
                return false;
            }
        });
        lv.setAdapter(simpleAdapter);

        presetWordsDropdown = (Spinner) findViewById(R.id.presetWordsDropdown);
        presetWords = new ArrayList<String>();
        presetWords.add("Crutch words");
        presetWords.add("Negative words");
        presetWords.add("Custom words");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, presetWords);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetWordsDropdown.setAdapter(dataAdapter);

        presetWordsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = presetWords.get(position);
                input_list.clear();
                if (selectedItem.equals("Negative words")) {
                    for (String negativeWord : new String[]{"dumb", "loser", "stupid", "terrible"}) {
                        HashMap<String, String> newInput = new HashMap<String, String>();
                        newInput.put("word", negativeWord);
                        newInput.put("dumb", "");
                        input_list.add(newInput);
                    }
                } else if (selectedItem.equals("Crutch words")) {
                    for (String crutchWord : new String[]{"basically", "like", "literally", "so", "umm", "yeah"}) {
                        HashMap<String, String> newInput = new HashMap<String, String>();
                        newInput.put("word", crutchWord);
                        newInput.put("dumb", "");
                        input_list.add(newInput);
                    }
                }
                simpleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void createDictionary() {
        dictionaryList = new ArrayList<String>();
        try {
            Assets assets = new Assets(DefineKeywordsActivity.this);
            File assetsDir = assets.syncAssets();
            BufferedReader br = new BufferedReader(new FileReader(new File(assetsDir, "models/dict/cmu07a.dic")));
            String line;
            while ((line = br.readLine()) != null) {
                String newWord = line.toString().split("\t")[0];
                dictionaryList.add(newWord);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean searchList(ArrayList dictionaryList, String input_txt) {
        int index = Collections.binarySearch(dictionaryList, input_txt);
        Log.d("index", Integer.toString(index));
        return (index >= 0);
    }

    public void savetext(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        String input_txt = input_text.getText().toString();
        input_text.getText().clear();
        if (searchList(dictionaryList, input_txt)) {
            HashMap<String, String> newInput = new HashMap<String, String>();
            newInput.put("word", input_txt);
            newInput.put("dumb", "");
            input_list.add(newInput);
            simpleAdapter.notifyDataSetChanged();
        } else if (!input_txt.equals("")) {
            Toast toast = Toast.makeText(this, "Word not in dictionary, sorry!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    public void submit(View v) {
        try {
            Assets assets = new Assets(DefineKeywordsActivity.this);
            File assetsDir = assets.syncAssets();
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(assetsDir, "commands.lst")));
            for (int i = 0; i < input_list.size(); i++) {
                String newWord = input_list.get(i).get("word");
                float threshold = 0;
                if (newWord.equals("umm") || newWord.equals("um") ||
                        newWord.equals("uh") || newWord.equals("uhh")) {
                    threshold = 2;
                } else if (newWord.length() <= 3) {
                    threshold = 5e-2f;
                } else if (newWord.length() > 3 && newWord.length() < 7) {
                    threshold = 1e-4f;
                } else {
                    threshold = 1e-6f;
                }
                bw.write(input_list.get(i).get("word") + " /" + Float.toString(threshold) + "/");
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent listenIntent = new Intent(this, ListenActivity.class);
        startActivity(listenIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }
}
