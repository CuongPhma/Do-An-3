#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <SPI.h>
#include <MFRC522.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>
#include <ESP32Servo.h>

// ==== Servo Config ====
Servo myservo;
int servoPin = 27;
int openPos = 90;
int closePos = 0;

void setupServo() {
  myservo.setPeriodHertz(50);
  myservo.attach(servoPin, 500, 2400);
  myservo.write(closePos);
}

void openDoor() {
  Serial.println("🔓 Mở cửa: Quay servo đến 90 độ");
  myservo.write(openPos);
  delay(3000);
  Serial.println("🔒 Đóng cửa: Quay servo về 0 độ");
  myservo.write(closePos);
}

// ==== DHT Config ====
#define DHTPIN 5
#define DHTTYPE DHT11
DHT_Unified dht(DHTPIN, DHTTYPE);

// ==== LED Config ====
#define RED_PIN    32
#define GREEN_PIN  25
#define BLUE_PIN   26

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

// ==== millis() Control ====
unsigned long previousMillisRFID = 0;
unsigned long previousMillisDHT = 0;
const unsigned long intervalRFID = 500;
const unsigned long intervalDHT = 60000;

// ==== Function Prototypes ====
void setColor(int r, int g, int b);
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
  Serial.println("Kết nối Firebase thành công");

  SPI.begin();
  rfid.PCD_Init();
  Serial.println("👉 Quét thẻ RFID...");

  dht.begin();
  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  dht.humidity().getSensor(&sensor);

  setupServo();

  // Cấu hình LED RGB
  pinMode(RED_PIN, OUTPUT);
  pinMode(GREEN_PIN, OUTPUT);
  pinMode(BLUE_PIN, OUTPUT);
  setColor(0, 0, 0); // Tắt đèn ban đầu
}

void loop() {
  unsigned long currentMillis = millis();

  if (currentMillis - previousMillisRFID >= intervalRFID) {
    previousMillisRFID = currentMillis;
    RC522();
  }

  if (currentMillis - previousMillisDHT >= intervalDHT) {
    previousMillisDHT = currentMillis;
    DHT_11();
  }

  
}

// ==== Điều khiển LED bằng digitalWrite (RGB cơ bản) ====
void setColor(int r, int g, int b) {
  digitalWrite(RED_PIN, r ? HIGH : LOW);
  digitalWrite(GREEN_PIN, g ? HIGH : LOW);
  digitalWrite(BLUE_PIN, b ? HIGH : LOW);
}

// ==== Quét RFID ====
void RC522() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return;

  String uidString = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    uidString += String(rfid.uid.uidByte[i] < 0x10 ? "0" : "");
    uidString += String(rfid.uid.uidByte[i], HEX);
  }
  uidString.toUpperCase();
  Serial.print("📛 UID: ");
  Serial.println(uidString);

  String path = String("/rfid/") + uidString;
  if (Firebase.RTDB.getBool(&fbdo, path)) {
    if (fbdo.boolData()) {
      Serial.println("✅ UID hợp lệ - Mở cửa");
      setColor(0, 1, 0); // Xanh lá
      openDoor();
      setColor(0, 0, 0); // Tắt
    } else {
      Serial.println("🚫 UID không hợp lệ!");
      setColor(1, 0, 0); // Đỏ
      delay(2000); // Đợi 2 giây trước khi tắt đèn
      setColor(0, 0, 0); // Tắt đèn
    }
  } else {
    Serial.print("❌ Lỗi truy vấn UID: ");
    Serial.println(fbdo.errorReason());
    setColor(1, 0, 0); // Đỏ
    delay(2000); // Đợi 2 giây trước khi tắt đèn
    setColor(0, 0, 0); // Tắt đèn
  }

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
}

// ==== Đọc DHT11 ====
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
