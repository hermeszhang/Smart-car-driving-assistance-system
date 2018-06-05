package com.example.nickwang.car;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 安卓控制程序
 * 1.负责通过wifi从ESP8266接收传输的数据
 * 2.将数据进行结构化
 * 3.对结构化的数据进行处理
 * 4.将处理结果通过wifi传给ESP8266
 *
 * @author nickwang
 */

public class MainActivity extends AppCompatActivity {
    /**
     * 常量声明
     */
    private final String FILENAME = "ESP8266Data.txt";
    private final int CONNECT_SUCCESS = 1;
    private final int CONNECT_FIAL = 2;
    private final int GET_ENOUGH_DATA = 3;

    /**
     * 控件声明
     */
    private Button connectToServerButton;
    private EditText serverIP;
    private EditText serverHost;
    private Switch receive_data_switch;

    /**
     * 变量声明
     */
    private Socket socket; // 用于TCP连接的socket
    private Queue<String> stringDataFromServer; // 存储从服务器接收的字符串序列
    private Queue<DataStruct> data; // 存储结构化的数据
    private boolean continueReceive = true; // 用来控制是否继续接收
    public Handler processDataHandler; // 数据处理线程的Handler
    public Handler receiveDataHandler; // 



    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CONNECT_SUCCESS:
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    break;
                case CONNECT_FIAL:
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 绑定控件
         */
        connectToServerButton = (Button) findViewById(R.id.connect_to_server_button);
        serverIP = (EditText) findViewById(R.id.server_ip);
        serverHost = (EditText) findViewById(R.id.server_host);
        receive_data_switch = (Switch) findViewById(R.id.receive_data_swh);

        /**
         * 初始化变量
         */
        stringDataFromServer = new LinkedList<String>();
        data = new LinkedList<DataStruct>();


        /**
         * 连接服务器
         */
        connectToServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Message message = new Message();
                            socket = new Socket(serverIP.getText().toString(), Integer.valueOf(serverHost.getText().toString()));
                            boolean isConnected = socket.isConnected();
                            if(isConnected){
                                message.what = CONNECT_SUCCESS;
                            }else {
                                message.what = CONNECT_FIAL;
                            }
                            handler.sendMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        receive_data_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    continueReceive = true; //可以接受数据
                    //接收数据
                    new Thread(receiveDataThread).start();
                    //处理数据
                    new Thread(processDataThread).start();
                }else{
                    //关闭接收数据
                    continueReceive = false; //不接受数据
                    //将数据写入文件
                    if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                        System.out.println("SD卡不存在");
                        return;
                    }
                    File file = null;
                    try {
                        file = new File(Environment.getExternalStorageDirectory().getCanonicalFile() + FILENAME);
                        FileOutputStream fos = new FileOutputStream(file);
                        System.out.println("正在写入文件...");
                        while (data.size() > 0){
                            String line = data.poll().toString();
                            fos.write(line.getBytes());
                        }
                        fos.close();
                        Toast.makeText(MainActivity.this, "写入文件完成", Toast.LENGTH_SHORT).show();
                        System.out.println("文件写入完成");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



    }


    Runnable receiveDataThread = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            receiveDataHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                    }
                }
            };

            System.out.println(continueReceive);
            if(continueReceive){
                try {
                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String str = null;
                    while((str = br.readLine()) != null && continueReceive){
                        stringDataFromServer.add(str);
                        if(stringDataFromServer.size() > 500){
                            Message dataEnoughMsg = new Message();
                            dataEnoughMsg.what = GET_ENOUGH_DATA;
                            processDataHandler.sendMessage(dataEnoughMsg);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("接受失败");
                }
            }

            Looper.loop();
        }
    };

    Runnable processDataThread = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            processDataHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case GET_ENOUGH_DATA:
                            //每接受一个消息就意味着有500个数据到来，此时需要对这500个数据进行处理
                            String line = stringDataFromServer.poll();
                            while(line != null){
                                String[] stringAfterSplit = line.split(";");
                                DataStruct dataTemp = new DataStruct();
                                if(dataTemp.setStruct(stringAfterSplit)){
                                    data.add(dataTemp);
                                }
                                line = stringDataFromServer.poll();
                            }
                            System.out.println("已处理完一组数据" + data.size());
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };






}
