package com.example.shwetank.tcpdatatransfer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class AudioMainActivity extends AppCompatActivity {

    private boolean isEnable = false;
    private Handler mHandler = new Handler();
    private static final int LISTEN_SERVER_PORT = 9761;

    EditText mName;
    EditText mSendingData;

    TextView mReceivingData;

    Button mEnable;
    Button mSend;

    ListView mListView;

    private String RECEIVER_IP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findId();
        setClickListener();
        startToListen();

    }

    private void startToListen() {
        Thread startToListen = new Thread(new ListenDataOnPort());
        startToListen.start();
    }

    private void findId() {
        mSendingData = findViewById(R.id.et_data);
        mReceivingData = findViewById(R.id.tv_recieve_data);
        mEnable = findViewById(R.id.btn_enable);
        mName = findViewById(R.id.et_name);
        mSend = findViewById(R.id.btn_send);
        mListView = findViewById(R.id.peerList);
    }

    private void setClickListener() {

        mEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEnable) {
                    if (mName.getText().length() != 0) {
                        NetworkSniffTask networkSniffTask = new NetworkSniffTask(getApplicationContext());
                        networkSniffTask.execute();
                    } else {
                        Toast.makeText(getApplicationContext(), "Enter your name", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSendingData.getText().length() != 0) {
                    SendDataToReceiver sendDataToReceiver = new SendDataToReceiver();
                    sendDataToReceiver.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Check Peer to Connect", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private class ListenDataOnPort implements Runnable {

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(LISTEN_SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null == serverSocket) {
                Log.d("MainActivity", "serverSocket null");
            } else {
                AudioTrack track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, 2048, AudioTrack.MODE_STREAM);
                track.play();
                byte[] buf = new byte[2048];
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        if (null == socket) {
                            Log.d("MainActivity", "socket is null");
                        } else {
                            InputStream is = socket.getInputStream();
                            is.read(buf);
                            track.write(buf, 0, 2048);
                            /*InputStream is = socket.getInputStream();
                            BufferedReader r = new BufferedReader(new InputStreamReader(is));
                            final String s = r.readLine();
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mReceivingData.setText(s);
                                }
                            });
                            is.close();*/
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class SendDataToReceiver extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);
            audioRecorder.startRecording();
            byte[] buf = new byte[2048];

            while (true) {
                audioRecorder.read(buf, 0, 2048);
                try {
                    Socket socket = new Socket(RECEIVER_IP, LISTEN_SERVER_PORT);
                    OutputStream os = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.write(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    class NetworkSniffTask extends AsyncTask<Void, Void, ArrayList<String>> {

        private static final String TAG = "nstask";

        private WeakReference<Context> mContextRef;

        public NetworkSniffTask(Context context) {
            mContextRef = new WeakReference<Context>(context);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            Log.d(TAG, "Let's sniff the network");
            ArrayList<String> mIpList = new ArrayList<>();
            try {
                Context context = mContextRef.get();

                if (context != null) {

                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);


                    Log.d(TAG, "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d(TAG, "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d(TAG, "prefix: " + prefix);

                    for (int i = 0; i < 255; i++) {
                        String testIp = prefix + String.valueOf(i);

                        InetAddress address = InetAddress.getByName(testIp);
                        boolean reachable = address.isReachable(200);

                        if (reachable) {
                            mIpList.add(String.valueOf(testIp));
                            Log.i(TAG, "Host: " + "(" + String.valueOf(testIp) + ") is reachable!");
                        } else {
                            Log.i(TAG, "Host: " + "(" + String.valueOf(testIp) + ") is NON reachable!");
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Well that's not good.", t);
            }

            return mIpList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            setList(strings);
        }
    }

    private void setList(final ArrayList<String> strings) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(AudioMainActivity.this, android.R.layout.simple_list_item_1, strings);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RECEIVER_IP = strings.get(position);
            }
        });
    }

}

