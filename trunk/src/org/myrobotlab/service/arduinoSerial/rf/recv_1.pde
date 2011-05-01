#include <VirtualWire.h>  // you must download and install the VirtualWire.h to your hardware/libraries folder
#define NOP  255

void setup()
{
  Serial.begin(115200);        // connect to the serial port    

// Initialise the IO and ISR
    vw_set_ptt_inverted(true);    // Required for RX Link Module
    vw_setup(1000);                   // Bits per sec
    vw_set_rx_pin(2);           // We will be receiving on pin 23 (Mega) ie the RX pin from the module connects to this pin. 
    vw_rx_start();                      // Start the receiver 
}

int loopCount = 0;

uint8_t buf[VW_MAX_MESSAGE_LEN];
uint8_t buflen = VW_MAX_MESSAGE_LEN;

void loop()
{

    ++loopCount;
  
    if (vw_get_message(buf, &buflen)) // check to see if anything has been received
    {
       int i;
       // Message with a good checksum received.
       
       if ((buf[0] != NOP) || (loopCount%30 == 0))
       {
         for (i = 0; i < buflen; i++)
         {
            Serial.print("|");
            Serial.print(buf[i],HEX);  // the received data is stored in buffer
         }
  
         Serial.println("");
       }
       
     }
}

