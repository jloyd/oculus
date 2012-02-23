/*
ocuLED_Light


ASCII Serial Commands
All 2 byte pairs, except for GET_VERSION

SPOT_ON = 's', [0-255] (intensity) 
FLOOD_ON = 'd', [0-255] (intensity) 
ECHO_ON = 'e', '1' (echo command back TRUE)
ECHO_OFF = 'e', '0' (echo command back FALSE)
GET_VERSION = 'y'

*/
 
const int lightPinA = 3;    
const int lightPinB = 11;   
const int dockLightPin = 5; 

boolean echo = false;

// buffer the command in byte buffer 
const int MAX_BUFFER = 8;
int buffer[MAX_BUFFER];
int commandSize = 0;

//boolean lightOn = false;
//int buttonState = 0;  
 
void setup() {                
	pinMode(lightPinA, OUTPUT);     
	pinMode(lightPinB, OUTPUT);
	pinMode(dockLightPin, OUTPUT);


	//overide default PWM freq
	TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM20); // phase correct (1/2 freq)
	//TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM21) | _BV(WGM20); // 'fast pwm' (1x freq)
	//TCCR2B = _BV(CS22) | _BV(CS21) | _BV(CS20); // divide by 1024 
	TCCR2B = _BV(CS22) | _BV(CS20); // divide by 128 
	//TCCR2B = _BV(CS21) | _BV(CS20); // divide by 8 
	OCR2A = 0; 
	OCR2B = 0; 

	//pinMode(buttonPin, INPUT);
	Serial.begin(57600);
	Serial.println("<reset>");
}

void loop() {

  if( Serial.available() > 0 ){
    // commands take priority 
    manageCommand(); 
  } 

}

void manageCommand(){

  int input = Serial.read();

  // end of command -> exec buffered commands 
  if((input == 13) || (input == 10)){
    if(commandSize > 0){
      parseCommand();
      commandSize = 0; 
    }
  } else {

    // buffer it 
    buffer[commandSize++] = input;

    // protect buffer
    if(commandSize >= MAX_BUFFER){
      commandSize = 0;
      // Serial.println("<overflow>");
    }
  }
}

void parseCommand(){

	if(buffer[0] == 'x'){
		Serial.println("<id:oculusLights>");
	}  
	if(buffer[0] == 'y') {
		Serial.println("<version:0.1.3>"); 
	} 
	if(buffer[0] == 's'){
    	OCR2A = buffer[1];
		OCR2B = buffer[1];
	}
	if(buffer[0] == 'd') {
		if(buffer[1]==0) { digitalWrite(dockLightPin, LOW); }
		else { digitalWrite(dockLightPin, HIGH); }
	}
	if(buffer[0] == 'e') {
		if(buffer[1] == '1')
			echo = true;
		if(buffer[1] == '0')
			echo = false ;
	} 

  // echo the command back 
	if(echo) { 
		Serial.print("<");
		Serial.print((char)buffer[0]);

		if(commandSize > 1)
			Serial.print(',');    

		for(int b = 1 ; b < commandSize ; b++) {
			Serial.print((String)buffer[b]);  
			if (b<(commandSize-1)) 
				Serial.print(',');    
		} 
		Serial.println(">");
	}
	
}
