package com.example.isa_3d.helloworld;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sdk.R;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.HeartRateQuality;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.app.Activity;
import android.view.View;
import android.os.AsyncTask;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;
import java.net.HttpURLConnection;

public class MainPage extends Activity {

    private BandClient client = null;
    TextView tvGSR;
    TextView tvHeartRate;
    TextView tvTemperature;
    TextView console;
    Switch trainingSwitch;

    //TextView day;
    //TextView hour;
    //TextView minute;
    //ToggleButton amPm;
    boolean am = true;

    private int intervalInSeconds = 30;
    private int normInterval = 30;  // Amount of time between readings during non-Hard Recording
    private int ADInterval = 1;     // Amount of time between readings during Hard Recording
    private int flagSplash = 10; // The amount of time in seconds that is marked before and
    // After a point of interest is placed.

    private float currentHR = -1;
    private float currentSkinTemp = -1;
    private int currentGSR = -1;
    private HeartRateQuality currentQuality = HeartRateQuality.ACQUIRING;
    private long currentTime;
    private long previousTime;
    private boolean ad;
    private boolean training = true;

    boolean hasConsent = false;
    Button btnConnect;
    Button btnConsent;
    //Button btnSetIP;
    Button btnAlert;
    TextView notepad;
    Button submitNote;
    Button btnRecord;
    boolean alertRed = true;
    boolean recording = false;
    TreeMap dataTree;
    TreeMap intervalData;
    private int serverResponseCode;
    private String selectedPath;
    private DataPoint data;
    private double HR;
    private int GSR;
    private double ST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main_page);

        // Connect these objects with the TextViews on the app's page
        tvGSR = (TextView) findViewById(R.id.tvGSR);
        tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        tvHeartRate = (TextView) findViewById(R.id.tvHeartRate);
        //day = (TextView) findViewById(R.id.editDay);
        //hour = (TextView) findViewById(R.id.editHour);
        //minute = (TextView) findViewById(R.id.editMinute);
        //amPm = (ToggleButton) findViewById(R.id.amPm);

        // This next section will allow us to get consent from the user to use the HR sensor
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        tvGSR.setText("");
        tvTemperature.setText("");
        tvHeartRate.setText("");
        new GsrSubscriptionTask().execute(); // Put first (runs connection)
        new TempSubscriptionTask().execute();
        new HRSubscriptionTask().execute();

        notepad = (TextView) findViewById(R.id.notepad);
        submitNote = (Button) findViewById(R.id.submitNote);
        submitNote.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Submit the note to a data structure or output to a file
                if (!notepad.getText().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainPage.this);
                    builder.setTitle("Submit Note?");
                    builder.setMessage("Are you sure you want to save this note?");
                    builder.setPositiveButton("          Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int which) {
                                    try {
                                        Date fn = new Date(System.currentTimeMillis());
                                        String[] fnData = fn.toString().split(" ");
                                        String[] timeData = fnData[3].toString().split(":");
                                        String FILENAME = "Note_" + fnData[2] + "_" + timeData[0] + "_" +
                                                timeData[1] + "_" + timeData[2] + ".txt";
//                                      File path = getFilesDir(); //change is made only for phones with internal storage only
                                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                        File file = new File(path, "Note.txt");

                                        path.mkdirs();

                                        BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
                                        out.write("[NOTE]-["+fnData[2]+"]["+fnData[3]+"]: ");
                                        out.write(notepad.getText().toString());

                                        out.write("\n\r");
                                        out.close();
                                        scanFile(file.getAbsolutePath());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }


                                    notepad.setText("");

                                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(MainPage.this);
                                    helpBuilder.setTitle("Note Sent");
                                    helpBuilder.setMessage("The note was saved successfully, thank you.");
                                    helpBuilder.setPositiveButton("Ok",
                                            new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Do nothing but close the dialog
                                                }
                                            });

                                    // Remember, create doesn't show the dialog
                                    AlertDialog hDialog = helpBuilder.create();
                                    hDialog.show();

                                }
                            });
                    builder.setNegativeButton("No          ", null);

                    AlertDialog helpDialog = builder.create();
                    helpDialog.show();
                }
            }
        });

        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
            }
        });

        trainingSwitch = (Switch) findViewById(R.id.trainingSwitch);
        trainingSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trainingSwitch.isChecked()) {
                    trainingSwitch.setText(trainingSwitch.getTextOn());
                    training = true;
                } else {
                    trainingSwitch.setText(trainingSwitch.getTextOff());
                    training = false;
                }
            }
        });

        // Update test is the "Reconnect" button on the screen we will use to connect to the sensors
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                tvGSR.setText("");
                tvTemperature.setText("");
                tvHeartRate.setText("");
                new GsrSubscriptionTask().execute(); // Put first (runs connection)
                new TempSubscriptionTask().execute();
                new HRSubscriptionTask().execute();
            }
        });

        btnAlert = (Button) findViewById(R.id.btnAlert);
        btnAlert.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!recording) {
                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(MainPage.this);
                    helpBuilder.setTitle("Not Recording");
                    helpBuilder.setMessage("To use this feature you must be recording data.\n" +
                            "Press the green button to start recording.");
                    helpBuilder.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing but close the dialog
                                }
                            });

                    // Remember, create doesn't show the dialog
                    AlertDialog helpDialog = helpBuilder.create();
                    helpDialog.show();
                }

                if (alertRed && recording) {
                    btnAlert.setBackgroundColor(Color.rgb(242, 200, 141));
                    alertRed = false;
                    ad = true;
                    intervalInSeconds = ADInterval;
                    btnAlert.setText("I am no longer feeling dysreflexic");
                } else if (recording){
                    btnAlert.setBackgroundColor(Color.rgb(242,228,141));
                    alertRed = true;
                    ad = false;
                    intervalInSeconds = normInterval;
                    btnAlert.setText("I am feeling dysreflexic");

                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(MainPage.this);
                    helpBuilder.setTitle("Feedback : Please select the severity of your dysreflexive moment");
                    String[] severeties = {"\n\nVery Severe\n","\n\nSevere\n","\n\nModerate\n","\n\nMild\n","\n\nVery Mild\n"};
                    //helpBuilder.setMessage("Select the severity of your dysreflexic moment.");
                    helpBuilder.setItems(severeties,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        Date fn = new Date(System.currentTimeMillis());
                                        String[] fnData = fn.toString().split(" ");
                                        String[] timeData = fnData[3].toString().split(":");
                                        String FILENAME = "Note_" + fnData[2] + "_" + timeData[0] + "_" +
                                                timeData[1] + "_" + timeData[2] + ".txt";
//                                      File path = getFilesDir(); //change is made only for phones with internal storage only
                                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                        File file = new File(path, "Note.txt");

                                        path.mkdirs();

                                        BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
                                        out.write("[ADSR]-["+fnData[2]+"]["+fnData[3]+"]: ");
                                        if (which == 0)
                                            out.write("Very Severe (5)");
                                        else if (which == 1)
                                            out.write("Severe (4)");
                                        else if (which == 2)
                                            out.write("Moderate (3)");
                                        else if (which == 3)
                                            out.write("Mild (2)");
                                        else if (which == 4)
                                            out.write("Very Mild (1)");
                                        else
                                            out.write("Unreported (X)");

                                        out.write(notepad.getText().toString());

                                        out.write("\n\r");
                                        out.close();
                                        scanFile(file.getAbsolutePath());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                    // Remember, create doesn't show the dialog
                    AlertDialog helpDialog = helpBuilder.create();
                    helpDialog.show();


                }
            }
        });

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {

                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(MainPage.this);
                    helpBuilder.setTitle("Stop Recording?");
                    helpBuilder.setMessage("Are you sure you want to stop recording data?");
                    helpBuilder.setPositiveButton("          Yes",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    recording = false;

                                    try {
                                        Date fn = new Date(System.currentTimeMillis());
                                        String[] fnData = fn.toString().split(" ");
                                        String[] timeData = fnData[3].toString().split(":");
                                        String FILENAME = "Data_" + fnData[2] + "_" + timeData[0] + "_" +
                                                timeData[1] + "_" + timeData[2] + ".txt";
                                        //String JFILENAME = "Data_" + fnData[2] + "_" + timeData[0] + "_" +
                                               //timeData[1] + "_" + timeData[2] + ".json";
//                                      File path = getFilesDir(); //change is made only for phones with internal storage only
                                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                        File file = new File(path, FILENAME);
                                        //File jfile = new File(path, JFILENAME);

                                        path.mkdirs();

                                        BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
                                        //BufferedWriter jout = new BufferedWriter(new FileWriter(jfile, true));

                                        //out.write("Data Recording Save " + timeToDate(System.currentTimeMillis()) + "\r\n");
                                        //out.write("----------------------------------------------------------------\r\n");
                                        //out.write("HR\tQuality\t\tGSR\tST\tAD\tDay   Time\r\n");
                                        //out.write("HR\tGSR\tST\tAD\tDay   Time\r\n");
                                        Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
                                        for (Map.Entry<Long, DataPoint> entry : entrySet) {
                                            DataPoint d = entry.getValue();

                                            Long l = entry.getKey();
                                            Date t = new Date(l);

                                            String notifier = "";
                                            if (d.getAD())
                                                notifier = " <<<";

                                            String[] dateData = t.toString().split(" "); // dow mon dd h:m:s: ts y
                                            out.write(String.format("%s :\t%s  %s%s\r\n",
                                                    d.toString(), dateData[2], dateData[3], notifier));
                                            //jout.write(d.toJSON().toString());
                                            data = entry.getValue();
                                            ST = data.getSkinTemp();
                                            GSR = data.getGSR();
                                            HR = data.getHeartRate();
                                            if (training)
                                                sendPostRequest();
                                        }
                                        out.write("\n\r");
                                        out.close();
                                        scanFile(file.getAbsolutePath());
                                        //scanFile(jfile.getAbsolutePath());
                                        //selectedPath = jfile.getAbsolutePath();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    btnRecord.setText("Start Recording Data");

                                    if (ad == true) {
                                        btnAlert.setBackgroundColor(Color.rgb(242, 228, 141));
                                        alertRed = true;
                                        ad = false;
                                        intervalInSeconds = normInterval;
                                        btnAlert.setText("I am feeling dysreflexic");
                                    }

                                }
                            });

                    helpBuilder.setNegativeButton("No          ", null);

                    AlertDialog helpDialog = helpBuilder.create();
                    helpDialog.show();
                } else {

                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(MainPage.this);
                    helpBuilder.setTitle("Start Recording?");
                    helpBuilder.setMessage("Are you sure you want to start recording data?");
                    helpBuilder.setPositiveButton("          Yes",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    dataTree = new TreeMap<Long, DataPoint>();
                                    recording = true;
                                    btnRecord.setText("Stop Recording Data");
                                }
                            });
                    helpBuilder.setNegativeButton("No          ", null);

                    AlertDialog helpDialog = helpBuilder.create();
                    helpDialog.show();
                }
            }
        });
        /*
        amPm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                am = !am;
            }
        });

        btnSetIP = (Button) findViewById(R.id.btnSetIP);
        btnSetIP.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int targetDay = Integer.parseInt(day.getText().toString());
                int targetMinute = Integer.parseInt(minute.getText().toString());
                int targetHour = Integer.parseInt(hour.getText().toString());

                if (recording && targetDay <= 31 && targetHour <= 24 && targetMinute <= 59) {
                    if (!am)
                        targetHour += 12;   // Adjust for hour

                    long middleTime = 0;

                    // Find the middle point
                    Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
                    for (Map.Entry<Long, DataPoint> entry : entrySet) {
                        DataPoint d = entry.getValue();

                        if (d.hour == targetHour && d.minute == targetMinute) {
                            middleTime = d.getTime();
                            break;
                        }
                    }

                    long buffer = 1000*flagSplash;

                    // Set in the AD tags
                    for (Map.Entry<Long, DataPoint> entry : entrySet) {
                        DataPoint d = entry.getValue();

                        if (d.getTime() < middleTime+buffer && d.getTime() > middleTime-buffer)
                            d.setAD(true);
                    }
                    //setInterestPoint(targetDay, targetHour, targetMinute);
                }
            }
        });
        */
    }   // End of onCreate()





    /**
     * Whenever we get a reading from the HR monitor, this listner will grab it's dataTree and
     * send it to be printed to the screen. Later on this is where we will send our dataTree
     * to our dataTreebase or dataTree structure
     */
    private BandHeartRateEventListener hrEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(BandHeartRateEvent event) {
            if (event != null) {

                currentHR = event.getHeartRate();
                currentQuality = event.getQuality();
                currentTime = System.currentTimeMillis();
                // Measured in beats per minute, we also sent the quality of the reading so that
                // The UI updater can inform us if the reading is bad.
                appendHRToUI(String.format("%d", event.getHeartRate()), event.getQuality());

                if (recording && (currentTime - previousTime > 1000 * intervalInSeconds)) {
                    previousTime = currentTime;
                    addNewDataPoint(currentTime);
                }
            }
        }
    };

    /**
     * Listener for the GSR (same as HR Listener)
     */
    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                // Measured in kiloOHMS
                currentGSR = event.getResistance();
                currentTime = System.currentTimeMillis();
                if (event.getResistance() <= 99000)
                    appendGSRToUI(String.format("%.2f", ((double)event.getResistance())/1000));
                else
                   appendGSRToUI(String.format("   --   "));

                if (recording && (currentTime - previousTime > 1000 * intervalInSeconds)) {
                    previousTime = currentTime;
                    addNewDataPoint(currentTime);
                }
            }
        }
    };

    /**
     * Listener for the skin temperature (same as HR listener)
     */
    private BandSkinTemperatureEventListener tempEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent event) {
            if (event != null) {
                // Changed the .getTemperature()'s celsius to fahrenheit
                currentSkinTemp = event.getTemperature();
                previousTime = currentTime;
                currentTime = System.currentTimeMillis();
                appendTempToUI(String.format("%.2f", event.getTemperature()*1.800 + 32.00));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Uncomment this along with the code in onPause() if you want to reset the
        // sensors each time you resume the application

        //tvGSR.setText("");
        //tvTemperature.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {

            // This is the code that would usually take away the current sensor listeners
            // Whenever the app is closed. I have commented this out so that we can continue
            // reading in dataTree from our current sensors. This way we only need to connect the
            // band until the next time the app is launched, not when it is resumed

         /*
            try {
                client.getSensorManager().unregisterGsrEventListener(mGsrEventListener);
                client.getSensorManager().unregisterSkinTemperatureEventListener(tempEventListener);
            } catch (BandIOException e) {
                appendGSRToUI(e.getMessage());
                appendTempToUI(e.getMessage());
            } */
        }
    }

    @Override
    protected void onDestroy() {

        // This is what runs when the app is terminated, if this happens then our dataTree reading
        // will stop and we will have to relaunch the app and reconnect
        if (client != null) {
            if (recording) {
                // Check if the tree actually has reasonable data
                recording = false;

                try {
                    Date fn = new Date(System.currentTimeMillis());
                    String[] fnData = fn.toString().split(" ");
                    String[] timeData = fnData[3].toString().split(":");
                    String FILENAME = "Data_"+fnData[2]+"_"+timeData[0]+"_"+
                            timeData[1]+"_"+timeData[2]+".txt";

                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(path, FILENAME);

                    path.mkdirs();

                    BufferedWriter out = new BufferedWriter(new FileWriter(file, true));

                    //out.write("Data Recording Save "+timeToDate(System.currentTimeMillis())+"\r\n");
                    //out.write("----------------------------------------------------------------\r\n");
                    //out.write("HR\tQuality\t\tGSR\tST\tAD\tDay   Time\r\n");
                    //out.write("HR\tGSR\tST\tAD\tDay   Time\r\n");
                    Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
                    for (Map.Entry<Long, DataPoint> entry : entrySet) {
                        DataPoint d = entry.getValue();

                        Long l = entry.getKey();
                        Date t = new Date(l);

                        String notifier = "";
                        if (d.getAD())
                            notifier = " <<<";

                        String[] dateData = t.toString().split(" "); // dow mon dd h:m:s: ts y
                        out.write(String.format("%s :\t%s  %s%s\r\n",
                                d.toString(), dateData[2], dateData[3], notifier));
                    }
                    out.write("\n\r");
                    out.close();
                    scanFile(file.getAbsolutePath());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                btnRecord.setText("Start Recording Data");
            } else {

                dataTree = new TreeMap<Long, DataPoint>();
                recording = true;
                btnRecord.setText("Stop Recording Data");
            }

            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

//    protected void onStop() {
//        if (recording) {
//            // Check if the tree actually has reasonable data
//            recording = false;
//
//            try {
//                Date fn = new Date(System.currentTimeMillis());
//                String[] fnData = fn.toString().split(" ");
//                String[] timeData = fnData[3].toString().split(":");
//                String FILENAME = "Data_"+fnData[2]+"_"+timeData[0]+"_"+
//                        timeData[1]+"_"+timeData[2]+".txt";
//
//                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                File file = new File(path, FILENAME);
//
//                path.mkdirs();
//
//                BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
//
//                out.write("Data Recording Save "+timeToDate(System.currentTimeMillis())+"\r\n");
//                out.write("----------------------------------------------------------------\r\n");
//                //out.write("HR\tQuality\t\tGSR\tST\tAD\tDay   Time\r\n");
//                out.write("HR\tGSR\tST\tAD\tDay   Time\r\n");
//                Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
//                for (Map.Entry<Long, DataPoint> entry : entrySet) {
//                    DataPoint d = entry.getValue();
//
//                    Long l = entry.getKey();
//                    Date t = new Date(l);
//
//                    String notifier = "";
//                    if (d.getAD())
//                        notifier = " <<<";
//
//                    String[] dateData = t.toString().split(" "); // dow mon dd h:m:s: ts y
//                    out.write(String.format("%s :\t%s  %s%s\r\n",
//                            d.toString(), dateData[2], dateData[3], notifier));
//                }
//                out.write("\n\r");
//                out.close();
//                scanFile(file.getAbsolutePath());
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            btnRecord.setText("Start Recording Data");
//        } else {
//
//            dataTree = new TreeMap<Long, DataPoint>();
//            recording = true;
//            btnRecord.setText("Stop Recording Data");
//        }
//
//        try {
//            client.disconnect().await();
//        } catch (InterruptedException e) {
//            // Do nothing as this is happening during destroy
//        } catch (BandException e) {
//            // Do nothing as this is happening during destroy
//        }
//
//        super.onStop();
//    }

//    public void finish() {
//        if (recording) {
//            // Check if the tree actually has reasonable data
//            recording = false;
//
//            try {
//                Date fn = new Date(System.currentTimeMillis());
//                String[] fnData = fn.toString().split(" ");
//                String[] timeData = fnData[3].toString().split(":");
//                String FILENAME = "Data_"+fnData[2]+"_"+timeData[0]+"_"+
//                        timeData[1]+"_"+timeData[2]+".txt";
//
//                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                File file = new File(path, FILENAME);
//
//                path.mkdirs();
//
//                BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
//
//                out.write("Data Recording Save "+timeToDate(System.currentTimeMillis())+"\r\n");
//                out.write("----------------------------------------------------------------\r\n");
//                //out.write("HR\tQuality\t\tGSR\tST\tAD\tDay   Time\r\n");
//                out.write("HR\tGSR\tST\tAD\tDay   Time\r\n");
//                Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
//                for (Map.Entry<Long, DataPoint> entry : entrySet) {
//                    DataPoint d = entry.getValue();
//
//                    Long l = entry.getKey();
//                    Date t = new Date(l);
//
//                    String notifier = "";
//                    if (d.getAD())
//                        notifier = " <<<";
//
//                    String[] dateData = t.toString().split(" "); // dow mon dd h:m:s: ts y
//                    out.write(String.format("%s :\t%s  %s%s\r\n",
//                            d.toString(), dateData[2], dateData[3], notifier));
//                }
//                out.write("\n\r");
//                out.close();
//                scanFile(file.getAbsolutePath());
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            btnRecord.setText("Start Recording Data");
//        } else {
//
//            dataTree = new TreeMap<Long, DataPoint>();
//            recording = true;
//            btnRecord.setText("Stop Recording Data");
//        }
//
//        try {
//            client.disconnect().await();
//        } catch (InterruptedException e) {
//            // Do nothing as this is happening during destroy
//        } catch (BandException e) {
//            // Do nothing as this is happening during destroy
//        }
//
//        super.finish();
//    }

    /**
     * This private class is the object we will use to check and make sure the GSR sensor is
     * obtainable.
     */
    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        appendGSRToUI(" ↻ ");
                        client.getSensorManager().registerGsrEventListener(mGsrEventListener);
                    } else {
                        appendGSRToUI("Error");
                    }
                } else {
                    appendGSRToUI("Error");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Error";
                        break;
                    default:
                        exceptionMessage = "Error";
                        break;
                }
                appendGSRToUI(exceptionMessage);

            } catch (Exception e) {
                appendGSRToUI(e.getMessage());
            }
            return null;
        }
    }

    /**
     * This private class is the object we will use to check and make sure the SkenTemp. sensor is
     * obtainable.
     */
    private class TempSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (true) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        appendTempToUI(" ↻ ");
                        client.getSensorManager().registerSkinTemperatureEventListener(tempEventListener);
                    } else {
                        appendTempToUI("Error");
                    }
                } else {
                    appendTempToUI("Error");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "SDK ∅";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Error";
                        break;
                    default:
                        exceptionMessage = "Error";
                        break;
                }
                appendTempToUI(exceptionMessage);

            } catch (Exception e) {
                appendTempToUI(e.getMessage());
            }
            return null;
        }
    }
    /*
    * This private class is the object we will use to check and make sure the HR sensor is
    * obtainable.
    */
    private class HRSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(hrEventListener);
                        hasConsent = true;
                    } else {
                        appendHRToUI(" ∅ ");
                    }
                } else {
                    appendHRToUI("Error");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "SDK ∅";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Error";
                        break;
                    default:
                        exceptionMessage = "Error";
                        break;
                }
                appendHRToUI(exceptionMessage);

            } catch (Exception e) {
                appendHRToUI(e.getMessage());
            }
            return null;
        }
    }

    /**
     * This is run when we want to check and ask if the person will give us consent to monitor their
     * heart rate
     */
    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                                if (consentGiven)
                                    hasConsent = true;

                                tvGSR.setText("");
                                tvTemperature.setText("");
                                tvHeartRate.setText("");
                                new GsrSubscriptionTask().execute(); // Put first (runs connection)
                                new TempSubscriptionTask().execute();
                                new HRSubscriptionTask().execute();
                            }
                        });
                    }
                } else {
                    appendHRToUI("Error\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "SDK ∅\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Error\n";
                        break;
                    default:
                        exceptionMessage = "Error";
                        break;
                }
                appendHRToUI(exceptionMessage);

            } catch (Exception e) {
                appendHRToUI(e.getMessage());
            }
            return null;
        }
    }

    /**
     * Updates the text of the HR sensor and updates the color
     * @param string
     * @param quality
     */
    private void appendHRToUI(final String string, final HeartRateQuality quality) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvHeartRate.setText(string);
                if (quality == HeartRateQuality.ACQUIRING) { // If it is aquiring
                    tvHeartRate.setTextColor(Color.rgb(200,30,30)); // Turn it red
                    tvGSR.setTextColor(Color.rgb(200,30,30));
                    tvTemperature.setTextColor(Color.rgb(200,30,30));
                } else {
                    tvHeartRate.setTextColor(((TextView) findViewById(R.id.bpm)).getTextColors()); // otherwise turn it grey
                    tvGSR.setTextColor(((TextView) findViewById(R.id.bpm)).getTextColors());
                    tvTemperature.setTextColor(((TextView) findViewById(R.id.bpm)).getTextColors());
                }
            }
        });
    }

    /**
     * Updates the text of the hr sensor only
     * @param string
     */
    private void appendHRToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvHeartRate.setText(string);
            }
        });
    }

    /**
     * Updates the GSR sensor text
     * @param string
     */
    private void appendGSRToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvGSR.setText(string);
            }
        });
    }

    /**
     * Updates the SkinTemp. sensor text
     * @param string
     */
    private void appendTempToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTemperature.setText(string);
            }
        });
    }

    /**
     * Checks to see if the band is connected and then tries to connect if needed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) { // if the band has not been synced to the object "client"
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) { // devices.length should be one, not zero
                appendGSRToUI("Error");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        // If the band isn't connected, then try to connect
        appendGSRToUI(" ↻ \n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private Date timeToDate(long timeToConvert) {
        Date d = new Date(timeToConvert);
        return d; // The long variable counts seconds since Jan 1 12:00 1970
    }

    public void addNewDataPoint(long d) {
        if (dataTree != null) {
            DataPoint p = new DataPoint
                    (currentTime,currentHR,currentQuality,currentGSR,currentSkinTemp,ad);
            dataTree.put(d, p);
        }
    }

    private void scanFile(String path) {

        MediaScannerConnection.scanFile(MainPage.this,
                new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    private boolean showYesNoPopUp(String title, String question, String yes, String no) {
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(question);
        boolean result = false;
        helpBuilder.setPositiveButton(yes,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return true;
    }



    public void sendPostRequest() {
        String test = " Shruthi says hi";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest myReq = new StringRequest(Request.Method.POST,
                "http://192.81.216.37:5000/upload",
                createMyReqSuccessListener(),
                createMyReqErrorListener()) {

            protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {
                Map<String, String> params = new HashMap<>();

                params.put("Size", "" + dataTree.size());

                String a = "0";
                String b = "0";
                if (data.getAD())
                    a = "1";
                if (data.getHRQ() == HeartRateQuality.LOCKED)
                    b = "1";

                String sGSR = ""+GSR;
                String sHR = ""+String.format("%.2f",HR);
                String sST = ""+String.format("%.2f",ST);

                params.put("ContactState", "1");
                params.put("GSR", sGSR);
                params.put("HR", sHR);
                params.put("ST", sST);
                params.put("ADState", "1");
                //edit this to include the different sensor values from the tree
                //either send as a single POST function or multiple POST functions
                /*
                Set<Map.Entry<Long, DataPoint>> entrySet = dataTree.entrySet();
                for (Map.Entry<Long, DataPoint> entry : entrySet) {
                    params.put("Size", "" + dataTree.size());
                    String a = "0";
                    String b = "0";

                    if (entry.getValue().getAD())
                        a = "1";
                    if (entry.getValue().getHRQ() == HeartRateQuality.LOCKED)
                        b = "1";

                    params.put("ContactState", b);
                    params.put("GSR", ""+entry.getValue().getGSR());
                    params.put("HR", ""+entry.getValue().getHeartRate());
                    params.put("ST", ""+entry.getValue().getSkinTemp());
                    params.put("ADState", a);
                } */

                return params;
            };
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };
        queue.add(myReq);
    }

    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                final TextView mTextView = (TextView) findViewById(R.id.showOutput);
                String test = "Error";
//                mTextView.setText(error.getMessage());
            }
        };
    }

    private Response.Listener<String> createMyReqSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Display the first 500 characters of the response string.
//                final TextView mTextView = (TextView) findViewById(R.id.showOutput);
//                String test = "acb";
//                mTextView.setText(response);
                String test = "Done";
            }
        };
    }







}
