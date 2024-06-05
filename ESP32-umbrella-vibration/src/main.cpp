/*
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleServer.cpp
    Ported to Arduino ESP32 by Evandro Copercini
    updates by chegewara
*/

#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#define VIBRATION_PIN 2
#define KEY_PIN 0
#define LED_PIN 15

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_SERVO_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

void vibration_1_time()
{
  digitalWrite(VIBRATION_PIN, HIGH);
  delay(1000);
  digitalWrite(VIBRATION_PIN, LOW);
}

void vibration_3_times()
{
  for (int i = 0; i < 3; i++)
  {
    vibration_1_time();
    delay(1000);
  }
}

void key_scan_task(void *arg);

class BLECallbacks : public BLECharacteristicCallbacks
{
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    std::string value = pCharacteristic->getValue();
    // 区分不同的特征值
    if (pCharacteristic->getUUID().equals(BLEUUID(CHARACTERISTIC_SERVO_UUID)))
    {
      if (value.length() > 0)
      {
        Serial.println("*********");
        if (value[0] == '0')
        {
          Serial.println("coming Call");
          vibration_3_times();
        }
        else if (value[0] == '1')
        {
          Serial.println("coming Message");
          vibration_1_time();
        }
        Serial.println();
        Serial.println("*********");
      }
    }
  }
};

void setup()
{
  Serial.begin(115200);
  BLEDevice::init("ESP32");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  BLECharacteristic *pCharacteristicServo = pService->createCharacteristic(
      CHARACTERISTIC_SERVO_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);

  pCharacteristicServo->setValue("0");
  pCharacteristicServo->setCallbacks(new BLECallbacks());

  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");

  pinMode(VIBRATION_PIN, OUTPUT);
  digitalWrite(VIBRATION_PIN, LOW);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  pinMode(KEY_PIN, INPUT_PULLUP);
  // // print address
  // BLEAddress address = BLEDevice::getAddress();
  // String addressStr = address.toString().c_str();
  // Serial.println(addressStr);

  xTaskCreate(key_scan_task, "key_scan_task", 1024 * 8, NULL, 5, NULL);
}

void blink_task(void *arg)
{
  while (1)
  {
    digitalWrite(LED_PIN, HIGH);
    delay(500);
    digitalWrite(LED_PIN, LOW);
    delay(500);
  }
}
enum led_status_e
{
  LED_OFF = 0,
  LED_ON = 1,
  LED_BLINK = 2
};

void key_scan_task(void *arg)
{
  // short press or long press
  int key_value = 0;
  int key_value_last = 0;
  int key_press_time = 0;
  int key_press_time_last = 0;
  xTaskHandle blink_task_handle;

  enum led_status_e led_status = LED_OFF;


  while (1)
  {
    if(led_status == LED_BLINK)
    {
      if(blink_task_handle == NULL)
      {
        xTaskCreate(blink_task, "blink_task", 1024, NULL, 5, &blink_task_handle);
      }
    }
    else if(led_status == LED_ON)
    {
      if(blink_task_handle != NULL)
      {
        vTaskDelete(blink_task_handle);
        blink_task_handle = NULL;
      }
      digitalWrite(LED_PIN, HIGH);
    }
    else if(led_status == LED_OFF)
    {
      if(blink_task_handle != NULL)
      {
        vTaskDelete(blink_task_handle);
        blink_task_handle = NULL;
      }
      digitalWrite(LED_PIN, LOW);
    }
    else
    {
      // do nothing
    }

    key_value = digitalRead(KEY_PIN);
    if (key_value == 0)
    {
      key_press_time++;
    }
    else
    {
      key_press_time_last = key_press_time;
      key_press_time = 0;
    }

    if (key_press_time_last > 0)
    {
      if (key_press_time_last < 300)
      {
        // short press
        Serial.println("short press");
        led_status = static_cast<led_status_e>((led_status + 1) % 3);
      }
      else
      {
        // long press
        Serial.println("long press");
        digitalWrite(LED_PIN, HIGH);
        delay(1000);
        digitalWrite(LED_PIN, LOW);
      }
      key_press_time_last = 0;
    }
    delay(10);
  }
}

void loop()
{
  // put your main code here, to run repeatedly:
  delay(2000);
}