#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <SPI.h>
#include <MFRC522.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>

// ==== DHT Config ====
#define DHTPIN 5 // Chân kết nối DHT
#define DHTTYPE DHT11
DHT_Unified dht(DHTPIN, DHTTYPE);


// ==== LED Config ====
#define RED_PIN    32
#define GREEN_PIN  25
#define BLUE_PIN   26
int red = 250;
int green = 250;
int blue = 250;

// ==== RFID Config ====
#define SS_PIN 21
#define RST_PIN 22
MFRC522 rfid(SS_PIN, RST_PIN);

// ==== WiFi Config ====
#define WIFI_SSID "A25859"
#define WIFI_PASSWORD "12345678"

// ==== Firebase Config ====
#define API_KEY "AIzaSyBc3SKMJKBHq2Q3_NONXaZRroQhpHbtBD8"
#define DATABASE_URL "https://do-an-3-9959f-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL "admin@gmail.com"
#define USER_PASSWORD "admin123"

// ==== Firebase Instances ====
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ==== Biến dùng để xử lý millis() thay thế scheduler ====
unsigned long previousMillisRFID = 0;
unsigned long previousMillisDHT = 0;
const unsigned long intervalRFID = 500;    // 1000ms quét RFID
const unsigned long intervalDHT = 60000;    // 15000ms đọc DHT11

// ==== Prototype ====
void dump_byte_array(byte *buffer, byte bufferSize);
void setColor(int r, int g, int b);
void changeColor(int r, int g, int b);
void RC522();
void DHT_11();


void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("🔌 Đang kết nối WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ WiFi đã kết nối!");


  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  Serial.println("Kết nối Firebase thành công ");
 

  SPI.begin();
  rfid.PCD_Init();
  Serial.println("👉 Quét thẻ RFID...");

  dht.begin();
  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  dht.humidity().getSensor(&sensor);

  pinMode(2, OUTPUT);
}

// ==== LOOP chính ====
void loop() {
  unsigned long currentMillis = millis();

  // Quét RFID mỗi 1000ms
  if (currentMillis - previousMillisRFID >= intervalRFID) {
    previousMillisRFID = currentMillis;
    RC522();
  }

  //Đọc cảm biến DHT11 mỗi 15000ms
  if (currentMillis - previousMillisDHT >= intervalDHT) {
    previousMillisDHT = currentMillis;
    DHT_11();
  }


  if (WiFi.status() != WL_CONNECTED) {
  Serial.println("🚫 WiFi mất kết nối! Đang kết nối lại...");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

}

// ==== Đổi màu RGB ====
void changeColor(int r, int g, int b) {
  ledcWrite(RED_PIN, r);
  ledcWrite(GREEN_PIN, g);
  ledcWrite(BLUE_PIN, b);
  red -= 5;
  green -= 5;
  blue -= 5;
  if (red < 0) red = 250;
  if (green < 0) green = 250;
  if (blue < 0) blue = 250;
}

void setColor(int r, int g, int b) {
  ledcWrite(RED_PIN, r);
  ledcWrite(GREEN_PIN, g);
  ledcWrite(BLUE_PIN, b);
}

// ==== Quét thẻ RFID ====
void RC522() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) {
    return;
  }

  Serial.print("📛 UID: ");
  for (byte i = 0; i < rfid.uid.size; i++) {
    Serial.print(rfid.uid.uidByte[i] < 0x10 ? "0" : "");
    Serial.print(rfid.uid.uidByte[i], HEX);
    Serial.print(" ");
  }
  // Gửi UID lên Firebase
  String uidString = "";
  for(byte i = 0; i < rfid.uid.size; i++) {
    uidString += String(rfid.uid.uidByte[i], HEX);
  }

  if (Firebase.RTDB.setString(&fbdo, "/rfid/uid", uidString)) {
    Serial.println(F("✅ UID đã gửi lên Firebase thành công"));
  } else {
    Serial.print(F("❌ Lỗi gửi UID: "));
    Serial.println(fbdo.errorReason());
  }

  // Thay đổi màu LED RGB
  changeColor(red, green, blue);

  
  Serial.println();

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  
}

// ==== Đọc và gửi dữ liệu cảm biến DHT11 ====
void DHT_11() {
  sensors_event_t event;

  dht.temperature().getEvent(&event);
  float temperature = event.temperature;

  dht.humidity().getEvent(&event);
  float humidity = event.relative_humidity;

  if (isnan(temperature) || isnan(humidity)) {
    Serial.println(F("❌ Lỗi đọc cảm biến!"));
  } else {
    Serial.print(F("🌡️ Nhiệt độ: "));
    Serial.print(temperature);
    Serial.println(F("°C"));
    Serial.print(F("💧 Độ ẩm: "));
    Serial.print(humidity);
    Serial.println(F("%"));

    if (Firebase.RTDB.setFloat(&fbdo, "/sensors/temperature", temperature) &&
        Firebase.RTDB.setFloat(&fbdo, "/sensors/humidity", humidity)) {
        Serial.println(F("✅ Gửi dữ liệu lên Firebase thành công"));
    } else {
      Serial.print(F("❌ Lỗi gửi dữ liệu DHT11: "));
      Serial.println(fbdo.errorReason());
    }
  }
}
