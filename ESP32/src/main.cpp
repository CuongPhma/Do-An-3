#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <SPI.h>
#include <MFRC522.h>
#include <MillisScheduler.h>
#include <Arduino.h> // Thư viện này thường được bao gồm tự động, nhưng nếu gặp lỗi có thể thêm vào
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>
MillisScheduler scheduler;

// ==== DHT Config ====
#define DHTPIN 5 // Chân kết nối DHT
#define DHTTYPE    DHT11     // DHT 11
DHT_Unified dht(DHTPIN, DHTTYPE);
uint32_t delayMS;

// ==== LED Config ====
#define RED_PIN    32
#define GREEN_PIN  25
#define BLUE_PIN   26 
int red = 250;
int green = 250;
int blue = 250;

// ==== RFID Config ====
#define SS_PIN 21   // SDA
#define RST_PIN 22  // RST
MFRC522 rfid(SS_PIN, RST_PIN);  // Tạo đối tượng RFID

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

void streamCallback(FirebaseStream data);
void streamTimeoutCallback(bool timeout);
void dump_byte_array(byte *buffer, byte bufferSize);
void setColor(int r, int g, int b);
void changeColor(int r, int g, int b);
void randomColor();
void RC522();
void DHT_11();
void SetUp_DHT11();
void SetUp_MFRC522();
void SetUp_FireBase();
void SetUp_WiFi();

void setup() {
  Serial.begin(115200);
  SetUp_WiFi(); // Kết nối WiFi
  SetUp_FireBase(); // Khởi tạo Firebase
  SetUp_MFRC522(); // Khởi tạo RFID
  SetUp_DHT11(); // Khởi tạo cảm biến DHT11

  pinMode(2, OUTPUT);

  //scheduler.addTask(200, RC522);   // 200ms quét thẻ RFID
  scheduler.addTask(1000, RC522); // 1000ms quét thẻ RFID
  scheduler.addTask(1000, DHT_11); // 1000ms đọc cảm biến DHT11
  
}

// ==== Callback khi Firebase có thay đổi ====
void streamCallback(FirebaseStream data) {
  if (data.dataType() == "string") {
    String status = data.stringData();
    Serial.println(status);

    if (status == "ON") {
      digitalWrite(2, HIGH);  // Bật đèn
      Serial.println("💡 ĐÈN BẬT");
    } else {
      digitalWrite(2, LOW);   // Tắt đèn
      Serial.println("💡 ĐÈN TẮT");
    }
  }
}

// ==== Callback khi stream timeout ====
void streamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println("⏳ Stream timeout, đang kết nối lại...");
  }
}

void loop() {
    
  scheduler.update(); // phải gọi trong loop
 
}

// Hàm đổi màu (Common Anode thì dùng 255 - value)
void changeColor(int r, int g, int b) {
 // Không cần đảo giá trị nữa
  analogWrite(RED_PIN, r);
  analogWrite(GREEN_PIN, g);
  analogWrite(BLUE_PIN, b);
  red -= 5;
  green -= 5;
  blue -= 5;
  if (red < 0) red = 250;
  if (green < 0) green = 250;
  if (blue < 0) blue = 250;
} 

void setColor(int r, int g, int b) {
  // Không cần đảo giá trị nữa
  analogWrite(RED_PIN, r);
  analogWrite(GREEN_PIN, g);
  analogWrite(BLUE_PIN, b);
}

void SetUp_WiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("🔌 Đang kết nối WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ WiFi đã kết nối!");
}

void SetUp_FireBase()
{
  /////////////////////////////////////////////////////////////////////////////
  // Cấu hình Firebase
  /////////////////////////////////////////////////////////////////////////////
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  // Bắt đầu lắng nghe thay đổi ở node /devices/den/status
  if (!Firebase.RTDB.beginStream(&fbdo, "/devices/den/status")) {
    Serial.println(fbdo.errorReason());
  }
  // Gán callback khi có thay đổi từ Firebase
  Firebase.RTDB.setStreamCallback(&fbdo, streamCallback, streamTimeoutCallback);
}

void SetUp_MFRC522() {
  // Khởi tạo các reader MFRC522
  SPI.begin();       // Khởi động SPI
  rfid.PCD_Init();   // Khởi động module RC522
  Serial.println("👉 Quét thẻ RFID...");
}
void RC522(){
 // Kiểm tra có thẻ mới không
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) {
    return;
  }

  // In UID của thẻ
  Serial.print("📛 UID: ");
  for (byte i = 0; i < rfid.uid.size; i++) {
    Serial.print(rfid.uid.uidByte[i] < 0x10 ? "0" : "");
    Serial.print(rfid.uid.uidByte[i], HEX);
    Serial.print(" ");
  }
  Serial.println();

  // Kết thúc giao tiếp với thẻ
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
}

// Hàm này sẽ được gọi định kỳ để thực hiện các tác vụ theo khoảng thời gian nhất định
// interval: khoảng thời gian giữa các lần thực hiện (tính bằng ms)
void Task(unsigned long interval, void (*func)()) {
  static unsigned long lastRun = 0;
  if (millis() - lastRun >= interval) {
    lastRun = millis();  // Cập nhật thời điểm thực hiện
    func();              // Gọi hàm truyền vào
  }
}


void SetUp_DHT11()
{
  // Initialize device.
  dht.begin();
  // Print temperature sensor details.
  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  // Print humidity sensor details.
  dht.humidity().getSensor(&sensor);
  // Set delay between sensor readings based on sensor details.
  delayMS = sensor.min_delay / 1000;
}
void DHT_11()
{
  // Get temperature event and print its value.
  sensors_event_t event;
  dht.temperature().getEvent(&event);
  if (isnan(event.temperature)) {
    Serial.println(F("Error reading temperature!"));
  }
  else {
    Serial.print(F("Temperature: "));
    Serial.print(event.temperature);
    Serial.println(F("°C"));
  }
  // Get humidity event and print its value.
  dht.humidity().getEvent(&event);
  if (isnan(event.relative_humidity)) {
    Serial.println(F("Error reading humidity!"));
  }
  else {
    Serial.print(F("Humidity: "));
    Serial.print(event.relative_humidity);
    Serial.println(F("%"));
  }
}