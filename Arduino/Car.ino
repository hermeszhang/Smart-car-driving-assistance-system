/**
   驾驶员汽车辅助系统

   ---------------MPU9250-------------------
   MPU9250传感器 --------- Arduino uno
   VDD ---------------------- 3.3V
   GND ---------------------- GND
   SDA ----------------------- A4
   SCL ----------------------- A5 


   ---------------4HC4051-------------------
   4HC4051－－Arduino Uno
   VCC－－3.3V
   GND－－GND
   Z  －－A5
   s0 －－8
   s1 －－9
   s2 －－10

   ---------------4HC4051-------------------
   4HC4051－－MPU9250
   VCC－－VCC
   GND－－GND
   Y0～Y7 －－SDA    

 */

#include "quaternionFilters.h"
#include "MPU9250.h"

MPU9250 myIMU1;
MPU9250 myIMU2;

#define AHRS false        // Set to false for basic data read
#define SerialDebug true  // Set to true to get Serial output for debugging

const int selectPins[3] = {8, 9, 10}; //s0-8, s1-9, s2-10
int sv = 0;

void setup() {
  Wire.begin();
  TWBR = 12;  // 400 kbit/sec I2C speed
  Serial.begin(115200); // Initialize the serial port

  // Set up the select pins as outputs:
  for (int i=0; i<3; i++)
  {
    pinMode(selectPins[i], OUTPUT);
    digitalWrite(selectPins[i], HIGH);
  }

  /*
   * IMU1
   * Start by performing self test and reporting values
   */
  myIMU1.MPU9250SelfTest(myIMU1.SelfTest);
  myIMU1.calibrateMPU9250(myIMU1.gyroBias, myIMU1.accelBias);
  myIMU1.initMPU9250();
  
  /*
   * IMU2
   * Start by performing self test and reporting values
   */
  myIMU2.MPU9250SelfTest(myIMU2.SelfTest);
  myIMU2.calibrateMPU9250(myIMU2.gyroBias, myIMU2.accelBias);
  myIMU2.initMPU9250();
}

void loop() {
  /*
   * IMU1
   */
  selectMuxPin(0);
  if (myIMU1.readByte(MPU9250_ADDRESS, INT_STATUS) & 0x01)
  {  
    myIMU1.readGyroData(myIMU1.gyroCount);
    myIMU1.getGres();

    myIMU1.gx = (float)myIMU1.gyroCount[0]*myIMU1.gRes;
    myIMU1.gy = (float)myIMU1.gyroCount[1]*myIMU1.gRes;
    myIMU1.gz = (float)myIMU1.gyroCount[2]*myIMU1.gRes; 
  } // if (readByte(MPU9250_ADDRESS, INT_STATUS) & 0x01)

  myIMU1.updateTime();

  if (!AHRS)
  {
    myIMU1.count = micros();
    if(SerialDebug)
    {
      Serial.print("IMU1;"+String(myIMU1.count)+";"
      +String(myIMU1.gx*3.1415926/180,5)+";"+String(myIMU1.gy*3.1415926/180,5)+";"+String(myIMU1.gz*3.1415926/180,5)+";"); 
    }
  }

  /**
   * IMU 2
   */
  selectMuxPin(1);
  if (myIMU2.readByte(MPU9250_ADDRESS, INT_STATUS) & 0x01)
  {  
    myIMU2.readAccelData(myIMU2.accelCount);
    myIMU2.getAres();

    myIMU2.ax = (float)myIMU2.accelCount[0]*myIMU2.aRes ;
    myIMU2.ay = (float)myIMU2.accelCount[1]*myIMU2.aRes ;
    myIMU2.az = (float)myIMU2.accelCount[2]*myIMU2.aRes ;

    myIMU2.readGyroData(myIMU2.gyroCount);
    myIMU2.getGres();

    myIMU2.gx = (float)myIMU2.gyroCount[0]*myIMU2.gRes;
    myIMU2.gy = (float)myIMU2.gyroCount[1]*myIMU2.gRes;
    myIMU2.gz = (float)myIMU2.gyroCount[2]*myIMU2.gRes;    
  } // if (readByte(MPU9250_ADDRESS, INT_STATUS) & 0x01)

  myIMU2.updateTime();
  if (!AHRS)
  {
    if(SerialDebug)
    {
      Serial.print("IMU2;"
      +String(myIMU2.ax*9.8,5)+";"+String(myIMU2.ay*9.8,5)+";"+String(myIMU2.az*9.8,5)+";"
      +String(myIMU2.gx*3.1415926/180,5)+";"+String(myIMU2.gy*3.1415926/180,5)+";"+String(myIMU2.gz*3.1415926/180,5)+";"); 
      }
  }

  /**
   * PPG
   */
   sv = analogRead(A0);
   Serial.print("PPG;");
   Serial.println(sv);
   delay(10);
}

void selectMuxPin(byte pin)
{
  for (int i=0; i<3; i++)
  {
    if (pin & (1<<i))
      digitalWrite(selectPins[i], HIGH);
    else
      digitalWrite(selectPins[i], LOW);
  }
}
