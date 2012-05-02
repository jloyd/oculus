/*
OcuLED 
send single byte commands:
x : hardware description (returns 'L' for 'lights')
y : firmware version
o : floodlight off
w : floodlight on
a-k : spotlight 0-100%
*/

const int lightPinA = 3;
const int lightPinB = 11;
const int dockLightPin = 5;

unsigned long lastcmd = 0;
int timeout = 30000;

void setup() {
 pinMode(lightPinA, OUTPUT);
 pinMode(lightPinB, OUTPUT);
 pinMode(dockLightPin, OUTPUT);

 //overide default PWM freq
 TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM20); // phase correct (1/2 freq)
 //TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM21) | _BV(WGM20); //'fast pwm' (1x freq)
 //TCCR2B = _BV(CS22) | _BV(CS21) | _BV(CS20); // divide by 1024
 TCCR2B = _BV(CS22) | _BV(CS20); // divide by 128
 //TCCR2B = _BV(CS21) | _BV(CS20); // divide by 8
 OCR2A = 0;
 OCR2B = 0;

 Serial.begin(57600);
 Serial.print('R');
}

void loop() {
 int input = 0;
 if( Serial.available() > 0 ){
   input = Serial.read();
   parseCommand(input);
   lastcmd = millis();
 }

 if (millis() - lastcmd > timeout) {
   lastcmd = millis();
   OCR2A = 0;
   OCR2B = 0;
   digitalWrite(dockLightPin, LOW);
 }
}

void parseCommand(int cmd){

 if(cmd == 'x'){
   Serial.print('L');
   return;
 }

 if(cmd == 'y'){
   Serial.print('1');
   return;
 }

 if(cmd == 'o'){
   digitalWrite(dockLightPin, LOW);
 }

 if(cmd == 'w'){
   digitalWrite(dockLightPin, HIGH);
 }

 if(cmd == 'a'){
   OCR2A = 0;
   OCR2B = 0;
 }
 else if(cmd == 'b'){
   OCR2A = 5;
   OCR2B = 5;
 }
 else if(cmd == 'c'){
   OCR2A = 33;
   OCR2B = 33;
 }
 else if(cmd == 'd'){
   OCR2A = 61;
   OCR2B = 61;
 }
 else if(cmd == 'e'){
   OCR2A = 88;
   OCR2B = 88;
 }
 else if(cmd == 'f'){
   OCR2A = 116;
   OCR2B = 116;
 }
 else if(cmd == 'g'){
   OCR2A = 144;
   OCR2B = 144;
 }
 else if(cmd == 'h'){
   OCR2A = 172;
   OCR2B = 172;
 }
 else if(cmd == 'i'){
   OCR2A = 199;
   OCR2B = 199;
 }
 else if(cmd == 'j'){
   OCR2A = 227;
   OCR2B = 227;
 }
 else if(cmd == 'k'){
   OCR2A = 255;
   OCR2B = 255;
 }

 Serial.print((char)cmd);

}
