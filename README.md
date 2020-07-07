#Intro
This project is a TLS terminator POC.

#Demo scripts

##Pub

 
###Unencrypted:
 
mosquitto_pub -p 1883 -t testT -m test8
 
###Encrypted:
 
mosquitto_pub --cafile /Users/amazonli/certs/ca.crt --insecure --cert /Users/amazonli/certs/client.crt --key /Users/amazonli/certs/client.key -p 8883 -t testT -m test7
 

##Subscribe

###Unencrupted
 
mosquitto_sub -p 1883 -t testT
 
###Encrypted
 
mosquitto_sub --cafile /Users/amazonli/certs/ca.crt --insecure --cert /Users/amazonli/certs/client.crt --key /Users/amazonli/certs/client.key -p 8883 -t testT
 
 

###Run Mosquito

/usr/local/sbin/mosquitto -c /usr/local/etc/mosquitto/mosquitto.conf
