package com.example.nickwang.car;

/**
 * 该类负责结构化服务器获取数据
 * @author nickwang
 */

public class DataStruct {
    private double timestamp;
    private double[] imu1; //3
    private double[] imu2; //6
    private int ppg;

    DataStruct(){
        imu1 = new double[3];
        imu2 = new double[6];
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double[] getImu1() {
        return imu1;
    }

    public void setImu1(double[] imu1) {
        this.imu1 = imu1;
    }

    public double[] getImu2() {
        return imu2;
    }

    public void setImu2(double[] imu2) {
        this.imu2 = imu2;
    }

    public int getPpg() {
        return ppg;
    }

    public void setPpg(int ppg) {
        this.ppg = ppg;
    }

    /**
     * 设置数据结构化
     * @param strTemp 传入的字符串数据
     * @return 返回该行数据是否为完整数据
     */
    public boolean setStruct(String[] strTemp){
        if(strTemp.length == 11){
            timestamp = Double.valueOf(strTemp[0]);
            imu1[0] = Double.valueOf(strTemp[1]);
            imu1[1] = Double.valueOf(strTemp[2]);
            imu1[2] = Double.valueOf(strTemp[3]);
            imu2[0] = Double.valueOf(strTemp[4]);
            imu2[1] = Double.valueOf(strTemp[5]);
            imu2[2] = Double.valueOf(strTemp[6]);
            imu2[3] = Double.valueOf(strTemp[7]);
            imu2[4] = Double.valueOf(strTemp[8]);
            imu2[5] = Double.valueOf(strTemp[9]);
            ppg = Integer.valueOf(strTemp[10]);
            return true;
        }else {
            System.out.println("该行数据格式出错，已忽略该行");
            return false;
        }
    }

    public static String setToString(DataStruct temp){
        return String.valueOf(temp.timestamp+";"
                +temp.imu1[0]+";" +temp.imu1[1]+";" +temp.imu1[2]+";"
                +temp.imu2[0]+";" +temp.imu2[1]+";" +temp.imu2[2]+";"
                +temp.imu2[3]+";" +temp.imu2[4]+";" +temp.imu2[5]+";"
                +temp.ppg +"\n");
    }

}
