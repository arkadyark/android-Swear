/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class ListenActivity extends Activity implements
        RecognitionListener {

    private static final String KWS_SEARCH = "wakeup";
    private static final String[] MONTHS = {"January", "February", "March", "April",
    "May", "June", "July", "August", "September", "October", "November", "December"};

    private SpeechRecognizer recognizer;
    private ArrayList<HashMap<String, String>> counters = new ArrayList<HashMap<String, String>>();
    private ArrayList<String> wordsToAvoid;
    private Vibrator vibr;
    private ListView lv;
    private TextView lastUpdatedTextView;
    private boolean listening = false;
    String prevText = "";
    SimpleAdapter simpleAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        //set up notitle
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //set up full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer...");

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        wordsToAvoid = new ArrayList<String>();
        counters = new ArrayList<HashMap<String, String>>();
        lv = (ListView) findViewById(R.id.listView);
        lastUpdatedTextView = (TextView) findViewById(R.id.lastUpdatedTextView);
        vibr = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        try {
            Assets assets = new Assets(ListenActivity.this);
            File assetDir = assets.syncAssets();
            loadWords(assetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(ListenActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Ready to listen!");
                    reset(null);
                }
            }
        }.execute();

    }

    private void loadWords(File assetsDir) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(assetsDir, "commands.lst")));
            String line;
            while ((line = br.readLine()) != null) {
                String newWord = line.toString().split(" ")[0];
                wordsToAvoid.add(newWord);
                HashMap<String, String> word = new HashMap<String, String>();
                word.put("Word", newWord);
                word.put("Count", "0");
                counters.add(word);
            }
            br.close();
            simpleAdapter = new SimpleAdapter(this,
                    counters,
                    R.layout.view_item,
                    new String[] {"Word", "Count"}, new int[] {R.id.textViewWord, R.id.textViewCount});
            lv.setAdapter(simpleAdapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();
        if (!(text.equals(prevText))) {
            vibr.vibrate(50);
            String newWord = text.substring(prevText.length()).trim();
            for (HashMap<String, String> word : counters) {
                if (word.get("Word").equals(newWord)) {
                    word.put("Count", Integer.toString(Integer.valueOf(word.get("Count")) + 1));
                    simpleAdapter.notifyDataSetChanged();
                }
            }
        }
        prevText = text;
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
    }

    public void toggleListening(View v) {
        if (listening) {
            recognizer.stop();
            listening = false;
            ((Button) findViewById(R.id.toggleButton)).setText("Start listening");
            ((TextView) findViewById(R.id.caption_text)).setText("Ready to listen!");
        } else {
            recognizer.stop();
            recognizer.startListening(KWS_SEARCH);
            listening = true;
            ((Button) findViewById(R.id.toggleButton)).setText("Stop listening");
            ((TextView) findViewById(R.id.caption_text)).setText("I'm listening");
            prevText = "";
        }
    }

    public void reset(View v) {
        Calendar rightNow = Calendar.getInstance();
        lastUpdatedTextView.setText("Last reset at " + rightNow.get(Calendar.HOUR_OF_DAY) + ":" +
                rightNow.get(Calendar.MINUTE) + " on " + MONTHS[Integer.valueOf(rightNow.get(Calendar.MONTH))] + " " +
                rightNow.get(Calendar.DAY_OF_MONTH));
        for (HashMap<String, String> word : counters) {
            word.put("Count", "0");
        }
        simpleAdapter.notifyDataSetChanged();
    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .getRecognizer();
        recognizer.addListener(this);
        // Create keyword-activation search.
        recognizer.addKeywordSearch(KWS_SEARCH, new File(assetsDir,
                "commands.lst"));
    }
}
