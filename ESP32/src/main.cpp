#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <SPI.h>
#include <MFRC522.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>
#include <ESP32Servo.h>

bool isBusy = false; // Biến toàn cục để kiểm soát trạng thái bận rộn

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
  isBusy = true; // Đặt trạng thái bận rộn
  Serial.println("🔓 Mở cửa: Quay servo đến 90 độ");
  myservo.write(openPos);
  delay(3000);
  Serial.println("🔒 Đóng cửa: Quay servo về 0 độ");
  myservo.write(closePos);
  isBusy = false; // Đặt lại trạng thái bận rộn
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

// ==== Buzzer Config ====
#define BUZZER 13
// ==== ChuyenDong Config ====
#define CHUYENDONG 34

// ==== WiFi Config ====
// #define WIFI_SSID "A25859"
// #define WIFI_PASSWORD "12345678"
#define WIFI_SSID "binmilo"
#define WIFI_PASSWORD "20202020"
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
unsigned long previousMillisReadToOpenDoor = 0;
unsigned long previousMillisReadNewCard = 0;
unsigned long previousMillisReadRequestNewCard = 0;
unsigned long previousMillisNightMode = 0;
const unsigned long intervalReadRequestNewCard = 1000; 
const unsigned long intervalReadNewCard = 5000; 
const unsigned long intervalReadToOpenDoor = 1000; // Thời gian servo hoạt động
const unsigned long intervalRFID = 500;
const unsigned long intervalDHT = 60000;
const unsigned long intervalNightMode = 2000; // Thời gian kiểm tra chế độ ban đêm

// ==== Function Prototypes ====
void setColor(int r, int g, int b);
void RC522();
void DHT_11();
void ReadFireBaseToOpenDoor();
void AddCard();
void checkNightMode();

void setup() {
  Serial.begin(115200);
  while (!Serial) {} // Chờ Serial kết nối
  
  pinMode(BUZZER, OUTPUT);
  digitalWrite(BUZZER, LOW); // Tắt Buzzer ban đầu
  pinMode(CHUYENDONG, INPUT);

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

  if (!isBusy &&currentMillis - previousMillisRFID >= intervalRFID) {
    previousMillisRFID = currentMillis;
    RC522();
  }

  if (!isBusy &&currentMillis - previousMillisDHT >= intervalDHT) {
    previousMillisDHT = currentMillis;
    DHT_11();
  }

  if (!isBusy &&currentMillis - previousMillisReadToOpenDoor >= intervalReadToOpenDoor) {
    previousMillisReadToOpenDoor = currentMillis;
    ReadFireBaseToOpenDoor();
  }
  
  if ( !isBusy && currentMillis - previousMillisReadRequestNewCard >= intervalReadRequestNewCard) {
    previousMillisReadRequestNewCard = currentMillis;
    AddCard();
  }

  if ( !isBusy && currentMillis - previousMillisNightMode >= intervalNightMode){
    previousMillisNightMode = currentMillis;
    checkNightMode();
  }
}

void checkNightMode() {
  String path_NightMode = "/night_mode/";

  if (Firebase.RTDB.getBool(&fbdo, path_NightMode)) {
    if (fbdo.boolData()) {
      // Kiểm tra nút Chuyển động
      if (digitalRead(CHUYENDONG) == LOW) {
        digitalWrite(BUZZER, HIGH); // Bật Buzzer
      }
      else if (digitalRead(CHUYENDONG) == HIGH){
        digitalWrite(BUZZER, LOW); // Tắt Buzzer
      }
    }
    else{
        digitalWrite(BUZZER, LOW); // Tắt Buzzer
    }
  }
}

// ==== Điều khiển LED bằng digitalWrite (RGB cơ bản) ====
void setColor(int r, int g, int b) {
  digitalWrite(RED_PIN, r ? HIGH : LOW);
  digitalWrite(GREEN_PIN, g ? HIGH : LOW);
  digitalWrite(BLUE_PIN, b ? HIGH : LOW);
}

// ==== Đọc FireBase để mở cửa ====
void ReadFireBaseToOpenDoor()
{
  if(Firebase.RTDB.getBool(&fbdo, "/door_control/status")) {
    if (fbdo.stringData() == "OPEN") {
      Serial.println("✅ Mở cửa từ Firebase");
      setColor(0, 1, 0); // Xanh lá
      openDoor();
      setColor(0, 0, 0); // Tắt
    } else {
      Serial.println("🚫 Không mở cửa từ Firebase");
    }
  } else {
    Serial.print("❌ Lỗi truy vấn Firebase: ");
    Serial.println(fbdo.errorReason());
    return;
  }
  // Reset giá trị mở cửa về false sau khi xử lý
  String Alert = "ALERT";
  if (Firebase.RTDB.setString(&fbdo, "/door_control/status", Alert)) {
    Serial.println("✅ Đặt lại trạng thái mở cửa về false");
  } else {
    Serial.print("❌ Lỗi đặt lại trạng thái mở cửa: ");
    Serial.println(fbdo.errorReason());
  }
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

  String path = String("/rfid/") + uidString + "/active";  // truy cập đến giá trị boolean

  if (Firebase.RTDB.getBool(&fbdo, path)) {
    if (fbdo.boolData()==true) {
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


void AddCard() {
  isBusy = true;
  previousMillisReadNewCard = millis(); // ✅ FIX lỗi thời gian

  String path_AddCard = "/new_card/";
  String path_CardList = "/rfid/";
  String name = "Thẻ mới";


  if (Firebase.RTDB.getBool(&fbdo, path_AddCard + "request")) {
    if (fbdo.boolData()) {
      Serial.println("🔁 Bắt đầu quá trình thêm thẻ mới...");
      setColor(1, 1, 0); // Vàng để báo đang chờ
      while (millis() - previousMillisReadNewCard < intervalReadNewCard) {
        Serial.println("⏳ Đang chờ thẻ...");
        if (!rfid.PICC_IsNewCardPresent()) {
          delay(300);
          continue;
        }
        if (!rfid.PICC_ReadCardSerial()) {
          delay(300);
          continue;
        }

        String newCardUID = "";
        for (byte i = 0; i < rfid.uid.size; i++) {
          newCardUID += String(rfid.uid.uidByte[i] < 0x10 ? "0" : "");
          newCardUID += String(rfid.uid.uidByte[i], HEX);
        }
        newCardUID.toUpperCase();

        String cardPath = path_CardList + newCardUID + "/active";
        if (Firebase.RTDB.getBool(&fbdo, cardPath) && fbdo.boolData()) {
          Serial.println("🚫 Thẻ đã tồn tại!");
          Firebase.RTDB.setBool(&fbdo, path_AddCard + "request", false);

          setColor(1, 0, 0); // Đỏ
          delay(2000);
          break;
        }

        if (Firebase.RTDB.getString(&fbdo, path_AddCard + "name")) {
          name = fbdo.stringData();
        }

        bool added = Firebase.RTDB.setBool(&fbdo, cardPath, true) &&
                     Firebase.RTDB.setString(&fbdo, path_CardList + newCardUID + "/name", name);

        if (added) {
          Serial.println("✅ Thêm thẻ mới thành công");
          setColor(0, 1, 0); // Xanh lá
          delay(2000);
          Firebase.RTDB.setBool(&fbdo, path_AddCard + "request", false);
        } else {
          Serial.print("❌ Lỗi thêm thẻ: ");
          Serial.println(fbdo.errorReason());
          setColor(1, 0, 0);
          delay(2000);
        }

        break; // ✅ Dừng vòng lặp sau khi xử lý
      }
      Firebase.RTDB.setBool(&fbdo, path_AddCard + "request", false);

       setColor(0, 0, 0); // Tắt đèn sau khi hoàn thành 
      rfid.PICC_HaltA();
      rfid.PCD_StopCrypto1();
    }
  } else {
    Serial.print("❌ Không đọc được yêu cầu thêm thẻ: ");
    Serial.println(fbdo.errorReason());
  }

  setColor(0, 0, 0); // Tắt đèn
  isBusy = false;
}
