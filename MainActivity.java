package com.message.whatapp.sonoff;


import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.mbms.MbmsErrors;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private TextView mTextViewTemp;
    private OkHttpClient client;

    private boolean reqFailed;
    private boolean status;
    private String temp;
    private Button Switch;
    private SwitchButton switchButton;
    private RelativeLayout AppLayout;
    private boolean InMiddleOfGetStatus;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitializeApp();
    }

    private void InitializeApp() {
        AppLayout = findViewById(R.id.AppLayout);
        mTextViewTemp = findViewById(R.id.temp_textview);
        client = new OkHttpClient();

        InitToggle();
        GetStatusFromServer(true);
        SetToggleListener();
        SetRefreshTimer();
    }

    private void SetToggleListener() {
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                //TODO do your job
                if (status != isChecked) {
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("status", !status);
                        status = !status;
                    } catch (Exception e) {
                    }
                    final String mRequestBody = jsonBody.toString();

                    SendPostReq(mRequestBody);
                }
            }
        });
    }

    private void InitToggle() {
        switchButton = (com.suke.widget.SwitchButton)
                findViewById(R.id.Switch);

        //switchButton.isChecked();
//        switchButton.toggle();     //switch state
//        switchButton.toggle(true);//switch without animation
        switchButton.setShadowEffect(true);//disable shadow effect
       // switchButton.setEnabled(true);//disable button
        switchButton.setEnableEffect(true);//disable the switch animation

    }

    private void SetRefreshTimer() {

        final long period = 10000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SetGUIData();
                GetStatusFromServer(false);
            }
        }, 0, period);
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        GetStatusFromServer(false);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        GetStatusFromServer(false);
    }


    private void ChangeButtonStatus(final boolean status) {
        if (switchButton.isChecked() != status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switchButton.setChecked(status);
                }
            });

        }

    }


    private void GetStatusFromServer(boolean first){
        if (!InMiddleOfGetStatus) SendGetReq("Status",first);
    }

    private void SetGUIData() {
        if (!reqFailed  && !InMiddleOfGetStatus) {
            SetTempComponenets();
            ChangeButtonStatus(status);
        }
        else if(reqFailed){
            SetErrorConnectToSever();
        }
    }

    private void SetTempComponenets() {
        try {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextViewTemp.setText(" \u2103" + temp);
                    if (Double.parseDouble(temp) < 18.0) {
                        AppLayout.setBackgroundColor(getResources().getColor(R.color.BlueSky));
                    } else {
                        AppLayout.setBackgroundColor(getResources().getColor(R.color.lightOrangeBtn));
                    }
                }
            });

        } catch (Exception e) {

        }
    }

    private void SendPostReq(String req){
        String url = "http://sonoff-arielu.ddns.net/api/switch";

        RequestBody body = RequestBody.create(req,JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                final String s = e.getMessage() ;
                SetErrorConnectToSever();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();

                }
                else{
                    SetErrorConnectToSever();
                }
            }
        });
    }
    private void SendGetReq(String req, final boolean first){
        String url = "http://sonoff-arielu.ddns.net/api/"+req;

        Request request = new Request.Builder().url(url).build();
        InMiddleOfGetStatus = true;
        reqFailed = false;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                InMiddleOfGetStatus = false;
                reqFailed = true;
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {
                    InMiddleOfGetStatus = false;
                    final String myResponse = response.body().string();
                    try {
                        JSONObject jsonBody = new JSONObject(myResponse);
                        status = Boolean.parseBoolean(jsonBody.getString("status"));
                        temp = jsonBody.getString("temp");
                    }
                    catch (Exception e){

                    }
                    if (first) SetGUIData();

                }
                else{
                    InMiddleOfGetStatus = false;
                    reqFailed = true;
                }
            }
        });
    }
    private void SetErrorConnectToSever(){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextViewTemp.setText("Error connecting to the server");
            }
        });
        ChangeButtonStatus(false);
    }
}



/*
// with Volley
//        final TextView textView = (TextView) findViewById(R.id.text_view_result);
//// ...
//
//// Instantiate the RequestQueue.
//        RequestQueue queue = Volley.newRequestQueue(this);
//        //String url ="http://www.google.com";
//        String url = "http://sonoff-arielu.ddns.net/api/status";
//
//// Request a string response from the provided URL.
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        // Display the first 500 characters of the response string.
//                        textView.setText("Response is: "+ response.substring(0,Math.min(500,response.length())));
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                textView.setText("That didn't work!");
//            }
//        });
//
//// Add the request to the RequestQueue.
//        queue.add(stringRequest);
        //Switch = findViewById(R.id.Switch);
 */