package com.dhrodriguezg.halloween.visionpassthrought;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.dhrodriguezg.halloween.visionpassthrought.entity.Server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends ActionBarActivity implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "MainActivity";

    public static Properties PREFERENCES;

    private EditText streamPort1;
    private EditText streamPort2;
    private EditText controlPort;
    private EditText phoneIP;
    private EditText serverIP;

    private Button startClient;
    private Button startServer;
    private Button testServer;
    private Button selectIP;

    private TreeMap<String,String> hostNameIPs;
    private MenuItem[] language;
    private PopupMenu deviceIps;
    private String publicIP;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        publicIP = null;
        updatePublicIP(true);
        findAllDeviceIPs();
        loadGUI();
    }

    private void loadGUI(){
        setContentView(R.layout.activity_main);

        streamPort1 = (EditText) findViewById(R.id.server_streamport1_value);
        streamPort2 = (EditText) findViewById(R.id.server_streamport2_value);
        controlPort = (EditText) findViewById(R.id.server_controlport_value);
        phoneIP = (EditText) findViewById(R.id.phone_ip_value);
        serverIP = (EditText) findViewById(R.id.client_serverip_value);

        startClient = (Button) findViewById(R.id.client_start_btn);
        startServer = (Button) findViewById(R.id.server_start_btn);
        testServer = (Button) findViewById(R.id.client_test_server_btn);
        selectIP = (Button) findViewById(R.id.phone_ip_btn);

        PREFERENCES = new Properties();

        if(hostNameIPs.containsKey( getString(R.string.server_default_comm) ))
            phoneIP.setText(hostNameIPs.get( getString(R.string.server_default_comm) ));
        else if (hostNameIPs.containsKey( getString(R.string.server_second_comm) ))
            phoneIP.setText(hostNameIPs.get(getString(R.string.server_second_comm)));
        else if (publicIP!=null)
            phoneIP.setText(publicIP);


        deviceIps = new PopupMenu( this, phoneIP);
        deviceIps.setOnMenuItemClickListener(this);
        int id = 0;
        for (String interfaces : hostNameIPs.keySet()){
            id++;
            deviceIps.getMenu().add(2, id, id, interfaces +": " + hostNameIPs.get(interfaces));
        }

        selectIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceIps.getMenu().clear();
                updatePublicIP(false);
                findAllDeviceIPs();
                int id=0;
                for (String interfaces : hostNameIPs.keySet()){
                    id++;
                    deviceIps.getMenu().add(2, id, id, interfaces +": " + hostNameIPs.get(interfaces));
                }
                deviceIps.show();
            }
        });

        testServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSendingPing();
            }
        });

        startClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startClientCameraActivity();
            }
        });

        startServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServerCameraActivity();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        language = new MenuItem[3]; //for now...
        language[0]= menu.add(1, 1, Menu.NONE, getString(R.string.english));
        language[1]= menu.add(1, 2, Menu.NONE, getString(R.string.spanish));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int gid = item.getGroupId();
        int iid = item.getItemId();

        if(gid==1 && iid==1){
            //english
            String languageToLoad  = "en";
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            loadGUI();
            return true;
        }else if(gid==1 && iid==2){
            //spanish
            String languageToLoad  = "es";
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            loadGUI();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getGroupId()) {
            case 2:
                phoneIP.setText(hostNameIPs.get(menuItem.getTitle().toString().split(":")[0]));
                return true;
            default:
                return false;
        }
    }

    private void startSendingPing(){
        int exit = pingHost(serverIP.getText().toString(), 5, true);
        if (exit!=0){
            Toast.makeText(getApplicationContext(), serverIP.getText().toString() + " is not reachable!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void startClientCameraActivity(){
        if (isMasterValid()){
            Server server = new Server(serverIP.getText().toString(), "name",
                    Integer.parseInt(streamPort1.getText().toString()),
                    Integer.parseInt(streamPort2.getText().toString()),
                    Integer.parseInt(controlPort.getText().toString()) );
            PREFERENCES.clear();
            PREFERENCES.put(getString(R.string.comm_mode_name), getString(R.string.comm_client_name));
            PREFERENCES.put(getString(R.string.comm_config_name), server);
            Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startServerCameraActivity(){
        Server server = new Server(phoneIP.getText().toString(), "name",
                Integer.parseInt(streamPort1.getText().toString()),
                Integer.parseInt(streamPort2.getText().toString()),
                Integer.parseInt(controlPort.getText().toString()) );
        PREFERENCES.clear();
        PREFERENCES.put(getString(R.string.comm_mode_name), getString(R.string.comm_server_name));
        PREFERENCES.put(getString(R.string.comm_config_name), server);

        Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    private void findAllDeviceIPs(){
        hostNameIPs = new TreeMap<String,String>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()){
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if ( !inetAddress.getHostAddress().contains("%") || !inetAddress.getHostAddress().contains(":")) {
                        hostNameIPs.put(networkInterface.getName(), inetAddress.getHostAddress());
                    }
                }
            }
            if(publicIP!=null)
                hostNameIPs.put(getString(R.string.public_ip_name), publicIP);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePublicIP(final boolean overrideHostIP){

        Thread thread = new Thread(){
            public void run(){
                try{
                    URL url = new URL("https://ifcfg.me/ip");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("POST");
                    conn.getResponseCode();
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    publicIP = org.apache.commons.io.IOUtils.toString(in, "UTF-8").trim();

                    if(overrideHostIP && serverIP != null){
                        runOnUiThread(new Runnable() {
                            public void run() {
                                serverIP.setText(publicIP);
                            }
                        });
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private boolean isMasterValid(){

        int exit = pingHost(serverIP.getText().toString(), 1, false);
        if (exit!=0){
            Toast.makeText(getApplicationContext(), serverIP.getText().toString() + " is not reachable!!!", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    //test socket
    private void testMaster(final String host, final int port){

        Thread thread = new Thread(){
            public void run(){
                int state=-1;
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 1000);
                    if(socket.isConnected()){
                        state=0;
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final int status = state;

                runOnUiThread(new Runnable() {
                    public void run() {
                        if(status==0)
                            Toast.makeText(getApplicationContext(), "Connection successful", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getApplicationContext(), "Can't connect to " + host + ":" + port + ". Is roscore running?", Toast.LENGTH_LONG).show();
                    }
                });
            }
        };
        thread.start();
    }

    private int pingHost(String host, int tries, boolean showStats){
        int exit = -1;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("ping -c " + tries + " " + host); // telnet ip 80
            proc.waitFor();
            if(showStats){
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                DecimalFormat numberFormat = new DecimalFormat("#.00");
                float minTime= Float.MAX_VALUE;
                float maxTime= Float.MIN_VALUE;
                float avgTime=0;
                float n=0.f;
                String result_line;
                while ((result_line = stdInput.readLine()) != null) {
                    if(!result_line.contains("time="))
                        continue;
                    String s_time = result_line.split("time=")[1].split(" ")[0];
                    float currentTime = Float.parseFloat(s_time);
                    avgTime += currentTime;
                    minTime = minTime < currentTime ? minTime : currentTime;
                    maxTime = maxTime > currentTime ? maxTime : currentTime;
                    n++;
                }
                avgTime/=n;
                Toast.makeText(getApplicationContext(), "Avg: " + numberFormat.format(avgTime) + "ms Min: " + numberFormat.format(minTime) + "ms Max: " + numberFormat.format(maxTime) + "ms", Toast.LENGTH_LONG).show();
            }
            exit = proc.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return exit;
    }

}
