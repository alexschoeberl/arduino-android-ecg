#define ANALOG_IN 0
#define BUTTON_PIN 2
#define LED_PIN 13

int value = 0;
int trash = 0;
unsigned long t_read = 0;
const unsigned long interval = 4999;

void setup() {
  delay(1000);
  analogReference(DEFAULT);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT);
  Serial.begin(57600);

  // Wait for manual start button
  digitalWrite(LED_PIN, LOW);
  while (digitalRead(BUTTON_PIN) == LOW) {
    delay(30);
  }
  digitalWrite(LED_PIN, HIGH);
  delay(1000);
}

void loop() {
  if ((micros()-t_read) > interval) {
    t_read = micros();
    value = analogRead(ANALOG_IN);
    Serial.write((value >> 8) & 0xff);
    Serial.write(value & 0xff);
    Serial.flush();
  }
}

// Test of sample rate precision
/*void loop() {
  if ((micros()-t_read) > interval) {
    value = (int)(micros() - t_read);
    t_read = micros();
    trash = analogRead(ANALOG_IN);
    Serial.write((value >> 8) & 0xff);
    Serial.write(value & 0xff);
    Serial.flush();
  }
}*/
