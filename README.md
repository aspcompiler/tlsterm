# Intro
This is a TLS terminating proxy example written in Java and Netty demonstrating connection to mosquitto.

# Demo scripts

## Pub

 
### Unencrypted:

```
mosquitto_pub -p 1883 -t testT -m test8
```

### Encrypted:

```
mosquitto_pub --cafile ${CERT_PATH}/ca.crt --insecure --cert ${CERT_PATH}/client.crt --key ${CERT_PATH}/client.key -p 8883 -t testT -m test7
```


## Subscribe

### Unencrupted

```
mosquitto_sub -p 1883 -t testT
```
 
### Encrypted

```
mosquitto_sub --cafile ${CERT_PATH}/ca.crt --insecure --cert ${CERT_PATH}/client.crt --key ${CERT_PATH}/client.key -p 8883 -t testT
```
 

### Run Mosquito

```
/usr/local/sbin/mosquitto -c /usr/local/etc/mosquitto/mosquitto.conf
```
