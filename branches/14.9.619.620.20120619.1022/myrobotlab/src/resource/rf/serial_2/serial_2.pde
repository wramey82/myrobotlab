#include <VirtualWire.h>  // you must download and install the VirtualWire.h to your hardware/libraries folder

// Transmitter Code

// RF Link using VirtualWire to Transmit messages
// simplex (one-way) receiver with a 315MHz RF Link Transmitter module
// tx pin 3 on Duemilanova (arduino)

unsigned long loopCount = 0;
int byteCount 		= 0;
int newByte 		= 0;
int readValue; 
int nibbleCount = 0;        // wii-led protocol


// communication types
#define HARDWARE_SERIAL 0
#define WIICOM 1
#define VIRTUAL_WIRE 2
#define NOP  255


int serialCommType = HARDWARE_SERIAL;
uint8_t ioCommand[3];
uint8_t data[3];
uint8_t nop[3];

void setup()
{
  
    Serial.begin(115200);        // connect to the serial port    
  
     // Initialise the IO and ISR
    vw_set_ptt_inverted(true); // Required for RF Link module
    vw_setup(1000);                 // Bits per sec
    vw_set_tx_pin(3);                // pin 3 is used as the transmit data out into the TX Link module, change this to suit your needs. 
    
    
    nop[0] = NOP;
    nop[1] = 8;
    nop[2] = 9;
}

boolean getCommand ()
{
  if (serialCommType == HARDWARE_SERIAL)
  { 
    // handle serial data begin 
    if (Serial.available () > 0)
    {
      // read the incoming byte:
      newByte = Serial.read();
      ioCommand[byteCount] = newByte;
      ++byteCount;

      if (byteCount > 2)
      {
        return true;
      }
    } // if Serial.available
  
    
  }
   
  return false;
    
}


void loop () {

  ++loopCount;
    
  if (getCommand())
  {
     // reverse
     data[0] = ioCommand[0];
     data[1] = ioCommand[1];
     data[2] = ioCommand[2];
     
     vw_send((uint8_t *)data, 3);
     vw_wait_tx();// TODO - remove                 

     // reset buffer
     ioCommand[0] = -1;
     ioCommand[1] = -1;
     ioCommand[2] = 0;
     byteCount = 0;           
     nibbleCount = 0;

  } else {

     // must stabalize RF comm with symmetric non-stop talking
     vw_send((uint8_t *)nop, 3);
     vw_wait_tx();// TODO - remove                 

  }

}


/*
// test loop
void loop()
{

   ++data[0];
   ++data[1];
   ++data[2];
   
   vw_send((uint8_t *)data, 3);
   vw_wait_tx();                                          // Wait for message to finish
//   delay(200);
   
}
*/
