package com.example.nickwang.car;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.concurrent.Semaphore;

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
    private final String FILENAME = "/ESP8266Data_";
    private final String[] FILE_TYPE = {"无压力", "低压力", "中压力", "高压力"};
    private final int NUM_ONCE_PROCESS = 500;
    private final int CONNECT_SUCCESS = 1;
    private final int CONNECT_FIAL = 2;
    private final int GET_ENOUGH_DATA = 3;
    private final int WRITE_DONE = 4;
    private final int RECEIVE_OFF = 5;
    private final int RECEIVE_ON = 6;
    private final int SAVE_FILE = 7;

    /**
     * 控件声明
     */
    private Button connectToServerButton;
    private EditText serverIP;
    private EditText serverHost;
    private Switch receiveDataSwitch;
    private Spinner fileSpinner;
    private Button saveFileButton;
    private EditText numOfTest;

    /**
     * 变量声明
     */
    private Socket socket; // 用于TCP连接的socket
    private Queue<String> stringDataFromServer; // 存储从服务器接收的字符串序列
    private Queue<DataStruct> data; // 存储结构化的数据
    private boolean continueReceive = true; // 用来控制是否继续接收
    private ArrayAdapter<String> adapterForSpinner;
    private Semaphore readAndWriteControlSemaphore; //信号量用于读写控制
    public Handler processDataHandler; // 数据处理线程的Handler
    public Handler receiveDataHandler; // 数据接受Handler
    public Handler writeDataToFileHandler; // 写文件Handler
    private String fileNameToSaveData;




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
                case WRITE_DONE:
                    Toast.makeText(MainActivity.this, "写入文件完成", Toast.LENGTH_SHORT).show();
                    //实验次数加一
                    int testNum = Integer.valueOf(numOfTest.getText().toString()) + 1;
                    numOfTest.setText(String.valueOf(testNum));
                    break;
                case RECEIVE_OFF:
                    Toast.makeText(MainActivity.this, "关闭数据采集", Toast.LENGTH_SHORT).show();
                    break;
                case RECEIVE_ON:
                    Toast.makeText(MainActivity.this, "开始数据采集", Toast.LENGTH_SHORT).show();
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
        receiveDataSwitch = (Switch) findViewById(R.id.receive_data_swh);
        fileSpinner = (Spinner) findViewById(R.id.file_type_spinner);
        saveFileButton = (Button) findViewById(R.id.save_file_btn);
        numOfTest = (EditText) findViewById(R.id.num_of_test);

        /**
         * 初始化变量
         */
        stringDataFromServer = new LinkedList<String>();
        data = new LinkedList<DataStruct>();
        readAndWriteControlSemaphore = new Semaphore(1); //同时间只有一个线程进行操作
        adapterForSpinner = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, FILE_TYPE);
        fileNameToSaveData = null;


        /**
         * 设置下拉列表
         */
        adapterForSpinner.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        fileSpinner.setAdapter(adapterForSpinner);
        fileSpinner.setVisibility(View.VISIBLE);
        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                numOfTest.setText("1");
                switch (position){
                    case 0:
                        fileNameToSaveData = FILENAME + "NO_PRESSURE_";
                        break;
                    case 1:
                        fileNameToSaveData = FILENAME + "LOW_PRESSURE_";
                        break;
                    case 2:
                        fileNameToSaveData = FILENAME + "MID_PRESSURE_";
                        break;
                    case 3:
                        fileNameToSaveData = FILENAME + "HIGH_PRESSURE_";
                        break;
                        default:
                            System.out.println("error");
                            break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        saveFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存
                Message message = new Message();
                message.what = SAVE_FILE;
                writeDataToFileHandler.sendMessage(message);
            }
        });

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

        receiveDataSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Message message = new Message();
                    message.what = RECEIVE_ON;
                    handler.sendMessage(message);
                    continueReceive = true; //可以接受数据
                    new Thread(receiveDataThread).start(); //接收数据
                    new Thread(processDataThread).start(); //处理数据
                    new Thread(writeDataToFileThread).start(); //写文件

                }else{
                    //关闭接收数据
                    continueReceive = false; //不接受数据
                    //将数据写入文件
                    Message message = new Message();
                    message.what = RECEIVE_OFF;
                    handler.sendMessage(message);
                }
            }
        });





    }

    /**
     * 线程：从服务器接收字符串数据，存储在stringDataFromServer中
     *      每接受500个数据向线程processDataThread发送一个GET_ENOUGH_DATA消息
     * 句柄：receiveDataHandler负责接收消息
     */
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
                        readAndWriteControlSemaphore.acquire();
                        stringDataFromServer.add(str);
                        readAndWriteControlSemaphore.release();
                        if(stringDataFromServer.size() > NUM_ONCE_PROCESS){
                            Message dataEnoughMsg = new Message();
                            dataEnoughMsg.what = GET_ENOUGH_DATA;
                            processDataHandler.sendMessage(dataEnoughMsg);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("接受失败");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Looper.loop();
        }
    };

    /**
     * 线程：每接收一个GET_ENOUGH_DATA消息，就将数据进行结构化处理
     *      将处理后的数据存储在data中
     * 句柄：processDataHandler
     */
    Runnable processDataThread = new Runnable() {
        String line = null;
        @Override
        public void run() {
            Looper.prepare();
            processDataHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case GET_ENOUGH_DATA:
                            //每接受一个消息就意味着有500个数据到来，此时需要对这500个数据进行处理
                            try {
                                for(int i = 0; i < NUM_ONCE_PROCESS; i++){
                                    readAndWriteControlSemaphore.acquire();
                                    line = stringDataFromServer.poll();
                                    readAndWriteControlSemaphore.release();
                                    if(line == null){
                                        continue;
                                    }else {
                                        String[] stringAfterSplit = line.split(";");
                                        DataStruct dataTemp = new DataStruct();
                                        if(dataTemp.setStruct(stringAfterSplit)){
                                            data.add(dataTemp);
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };

    /**
     * 线程：将数据写入文件
     * 句柄：writeDataToFileHandler
     */
    Runnable writeDataToFileThread = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            writeDataToFileHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    File file = null;
                    String pressureType = null;
                    switch (msg.what){
                        case SAVE_FILE:
                            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                                System.out.println("SD卡不存在");
                                return;
                            }
                            try {
                                file = new File(Environment.getExternalStorageDirectory().getCanonicalFile() + fileNameToSaveData + numOfTest.getText() + ".txt");
                                FileOutputStream fos = new FileOutputStream(file);
                                while (data.size() > 0){
                                    String line = DataStruct.setToString(data.poll());
                                    fos.write(line.getBytes());
                                }
                                fos.close();
                                Message message = new Message();
                                message.what = WRITE_DONE;
                                handler.sendMessage(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };
}
