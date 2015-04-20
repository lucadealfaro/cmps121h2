package com.dealfaro.luca.clicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    Location lastLocation = new Location("dummyprovider");

    private double lastAccuracy = (double) 1e10;
    private long lastAccuracyTime = 0;

    private static final String LOG_TAG = "local_msger";



    // This is an id for my app, to keep the key space separate from other apps.
    private static final String MY_APP_ID = "lcolling_msger";

    private static final String SERVER_URL_PREFIX = "https://luca-teaching.appspot.com/store/default/";


    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;
    private double curr_lat = 20.3;
    private double curr_long = 20.3;

    private ArrayList<String> accountList;

    private class ListElement {
        ListElement() {};

        public String msgid;
        public String msg;
        public String ts;
    }

    private ArrayList<ListElement> aList;

    private class MyAdapter extends ArrayAdapter<ListElement> {

        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            ListElement w = getItem(position);

            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource,  newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            //TextView tvMsgId = (TextView) newView.findViewById(R.id.itemMsgId);
            TextView tvMsg = (TextView) newView.findViewById(R.id.itemMsg);
            TextView tvTs = (TextView) newView.findViewById(R.id.itemTs);

            //tvMsgId.setText(w.msgid);
            tvMsg.setText(w.msg);
            tvTs.setText(w.ts);

            return newView;
        }
    }

    private MyAdapter aa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aList = new ArrayList<ListElement>();
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
        aa.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //init last location
        lastLocation.setLatitude(curr_lat);
        lastLocation.setLongitude(curr_long);
        updateLatLong();
        runInit();
    }

    //this function initalizes the listview with messages from the last known location
    public void runInit() {
        // Then, we start the call.
        GetMessageSpec myCallSpec = new GetMessageSpec();


        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();

        Location thisLocation = lastLocation;

        if (thisLocation == null) {
            Log.i(LOG_TAG, "Location has not been initialized, quitting");
            return;
        }


        double lat = curr_lat;
        double longit = curr_long;

        Log.i(LOG_TAG, "using this as lat " + lat);
        Log.i(LOG_TAG, "using this as long " + longit);

        m.put("lat", Double.toString(lat));
        m.put("lng", Double.toString(longit));

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Then start to request location updates, directing them to locationListener.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void updateLatLong() {
        TextView latView = (TextView) findViewById(R.id.latView);
        TextView longView = (TextView) findViewById(R.id.longView);

        latView.setText("lat: " + Double.toString(curr_lat));
        longView.setText("long: " + Double.toString(curr_long));
    }
    /**
     * Listenes to the location, and gets the most precise recent location.
     */
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
            Log.i(LOG_TAG, location.toString());
            curr_long = lastLocation.getLongitude();
            curr_lat = lastLocation.getLatitude();
            updateLatLong();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    @Override
    protected void onPause() {

        // Stops the location updates.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);

        // Disables the submit button.
        Button submitButton = (Button) findViewById(R.id.button);
        submitButton.setEnabled(false);

        // Stops the upload if any.
        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }
        super.onPause();
    }


    public void clickButton(View v) {

        // Get the text we want to send.
        EditText et = (EditText) findViewById(R.id.editText);
        String msg = et.getText().toString();
        if (msg.equals("")) return;

        startSpinner();

        Log.i(LOG_TAG, "submitting " + msg);
        // Then, we start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();


        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        myCallSpec.context = MainActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();

        Location thisLocation = lastLocation;

        if (thisLocation == null) {
            Log.i(LOG_TAG, "Location has not been initialized, quitting");
            return;
        }

        double lat = curr_lat;
        double longit = curr_long;

        m.put("msgid", reallyComputeHash(msg));
        m.put("msg", msg);

        m.put("lat", Double.toString(lat));
        m.put("lng", Double.toString(longit));

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    public void clickRefresh(View v) {
        // Then, we start the call.
        GetMessageSpec myCallSpec = new GetMessageSpec();
        startSpinner();

        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();

        Location thisLocation = lastLocation;

        if (thisLocation == null) {
            Log.i(LOG_TAG, "Location has not been initialized, quitting");
            return;
        }


        double lat = curr_lat;
        double longit = curr_long;

        Log.i(LOG_TAG, "using this as lat " + lat);
        Log.i(LOG_TAG, "using this as long " + longit);

        m.put("lat", Double.toString(lat));
        m.put("lng", Double.toString(longit));

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);

    }
    private void startSpinner() {
        ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);
    }
    private void stopSpinner() {
        ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.INVISIBLE);
    }

    private String reallyComputeHash(String s) {
        // Computes the crypto hash of string s, in a web-safe format.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            digest.update("My secret key".getBytes());
            byte[] md = digest.digest();
            // Now we need to make it web safe.
            String safeDigest = Base64.encodeToString(md, Base64.URL_SAFE);
            return safeDigest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();

                //now lets clear the edit text
                EditText et = (EditText) findViewById(R.id.editText);
                et.setText("");
            }
            stopSpinner();
        }
    }
    class GetMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
            stopSpinner();
        }
    }


    private void displayResult(String result) {
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);

        // Fills aList, so we can fill the listView.

        aList.clear();
        SimpleDateFormat  format = new SimpleDateFormat("MM-cc");

        for (int i = 0; i < ml.messages.length; i++) {
            Message m = ml.messages[i];
            String msg = m.getMsg();
            String ts = m.getTs();
            String msgid = m.getMsgid();


            String loggz = "msgid: " + msgid + " msg: " + msg + " ts: " + ts;
            Log.d(LOG_TAG, loggz);

            ListElement ael = new ListElement();
            //ael.msgid = "";
            ael.msg = msg;
            ael.ts = ts;
            aList.add(ael);

        }
        aa.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
