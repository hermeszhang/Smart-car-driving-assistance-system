/**
 * ESP8266设置AP
 * @auther nick wang
 * 
 * 修改日志
 * 
 */

#include <ESP8266WiFi.h>
#define MAX_SRV_CLIENTS 3

WiFiServer server(8266);
WiFiClient serverClients[MAX_SRV_CLIENTS];

const char WiFiAPPSW[] = "12345678"; //设置AP模式下模块所发出的WIFI的密码
char data[1500];
int ind = 0;

void setupWiFi()  
{  
  WiFi.mode(WIFI_AP);
  uint8_t mac[WL_MAC_ADDR_LENGTH];
  WiFi.softAPmacAddress(mac);
  
  String macID = String(mac[WL_MAC_ADDR_LENGTH - 3], HEX) +  
                 String(mac[WL_MAC_ADDR_LENGTH - 2], HEX) +  
                 String(mac[WL_MAC_ADDR_LENGTH - 1], HEX);  

  macID.toUpperCase();
  
  //设置AP模式下的WIFI名称：12345678_ + MAC地址后六位    
  String AP_NameString = "12345678" + macID;  

  char AP_NameChar[AP_NameString.length() + 1];  
  memset(AP_NameChar, AP_NameString.length() + 1, 0);  
     
  for (int i=0; i<AP_NameString.length(); i++)
  {
    AP_NameChar[i] = AP_NameString.charAt(i);
  }

  WiFi.softAP(AP_NameChar, WiFiAPPSW);  
   
  Serial.println();  
  Serial.print ( "IP address: " );  
  Serial.println ( WiFi.softAPIP() );  

  server.begin();      
}

void setup()   
{  
  Serial.begin(115200);  
  Serial.println ("\nBegin now!!!");  
  setupWiFi();  
}  

void loop()   
{  
    uint8_t i;
    /**
     * 检测服务器端是否有活动的客户端连接
     */
    if (server.hasClient())  
    {
      //查找空闲或者断开连接的客户端，并置为可用
      for (i = 0; i < MAX_SRV_CLIENTS; i++)
      {
          if (!serverClients[i] || !serverClients[i].connected())
          {
              if (serverClients[i])
              {
                serverClients[i].stop();  //未连接,就释放
              }
              serverClients[i] = server.available();  //分配新的
              continue;  
          }   
      }
      //若没有可用客户端，则停止连接  
      WiFiClient serverClient = server.available();  
      serverClient.stop();
    }  
    
    /**
     * 接受客户端的数据
     */
    for (i = 0; i < MAX_SRV_CLIENTS; i++)  
    {  
        if (serverClients[i] && serverClients[i].connected())  
        {
            if (serverClients[i].available())  
            {    
              //从Telnet客户端获取数据 
              while (serverClients[i].available()) 
              {
                  data[ind] = serverClients[i].read(); //读取client端发送的字符
                  ind++;
              }
              for(int j = 0; j < ind; j++)  
              {
                Serial.print(data[j]);  
              }
              
              Serial.println();  
              ind = 0;  
            }       
        }
    }

    /**
     * 向客户端发送数据
     */
    for (i = 0; i < MAX_SRV_CLIENTS; i++)  
    {  
        if (serverClients[i] && serverClients[i].connected())  
        {
            if (Serial.available())
            {
              size_t len = Serial.available();
              uint8_t str[len];
              Serial.readBytes(str, len);
              serverClients[i].write(str, len);
            }
        }  
    }
    
} 
