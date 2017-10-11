package com.ammarkureja.top10downloader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listApps = (ListView) findViewById(R.id.xmlListView);
        // feedlimit will replace %d in feedUrl
       downloadUrl(String.format(feedUrl, feedLimit));

    }

    // following two method are used to create menus
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mnuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " setting FeedLimit to " + feedLimit);
                }
                else {
                    Log.d(TAG, "onOptionsItemSelected: "+ item.getTitle() + " FeedLimit unchanged");
                }
                break;
            case R.id.mnu25:
            default:
                return super.onOptionsItemSelected(item);

        }


        // feedlimit will replace %d in feedUrl
        downloadUrl(String.format(feedUrl, feedLimit));
        return true;

    }
    //used to download from feed url;
    private void downloadUrl (String feedUrl) {
        Log.d(TAG, "downloadUrl: starting Asynctask");
        DownloadData downloadData = new DownloadData();
        downloadData.execute(feedUrl);
        Log.d(TAG, "downloadUrl: done");
    }

    // Async task run in background
    private class DownloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: parameter is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);
// its the builtin adaptor, we dont need our custom adopter for this.
//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<FeedEntry>(MainActivity.this,
//                    R.layout.list_item, parseApplications.getApplications());
//            listApps.setAdapter(arrayAdapter);

            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);
        }

        @Override
        //we can pass multiple [array of url's] in this method
        protected String doInBackground(String... strings) {
            //logd means debugg level. these entries removed automatically in production level
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            // we are only using first url thats why passing first value of strings array "strings[0]"
            String rssFeed = downloadXML(strings[0]);
            if (rssFeed == null) {
                //log.e means error, these entries remain in production level release of app
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        // this method used to read rss feed over the internet with a given url.
        private String downloadXML(String urlPath) {
            StringBuilder xmlResult = new StringBuilder();
            try {
                URL url = new URL(urlPath);
                //connection will not open until you get permission to access the internet in "menifest" file.
                //<uses-permission android:name="android.permission.INTERNET"/> this line need to be added in menifest file to get permission
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: The response code was" + response);
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader reader = new BufferedReader(inputStreamReader);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                //size of the readLine
                int charsRead;
                //used to store each line per loop
                char[] inputBuffer = new char[500];
                while (true) {
                    //its reading from buffer reader and saving it into inputBuffer
                    // and returning the number of charachters read into charRead
                    charsRead = reader.read(inputBuffer);
                    if (charsRead < 0) {
                        break;
                    }

                    if (charsRead > 0) {
                        xmlResult.append(inputBuffer, 0, charsRead);
                    }
                }
                reader.close();
                return xmlResult.toString();
            }
            //order of catching exception is important here. MalformatURLException is subclass of IOEXCEPTION.
            // if we call ioException first than we will not be able to see MalformatURLException.
            catch (MalformedURLException e) {
                Log.e(TAG, "downloadXML: Invalid URL " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "downloadXML: IO Exception reading data " + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "downloadXML: Security Exception. Needs permissions? " + e.getMessage());
                //       e.printStackTrace();
            }
            return null;
        }
    }
}
