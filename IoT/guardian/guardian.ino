#include <WiFi.h>
#include <PubSubClient.h>

#define RXD2 16 
#define TXD2 17  
#define botonPin 32

HardwareSerial A9G(2);

const char* ssid = "Comunidad Innovadores";
const char* password = "INn0V4-2K23!*";

const char* mqtt_server = "161.132.45.106";
const int mqtt_port = 1883;
const char* mqtt_topic = "gps/ubicacion";
const char* mqtt_topic_emergencia = "gps/emergencia";

WiFiClient espClient;
PubSubClient client(espClient);

float convertirCoordenada(String valor, String direccion) {
  float grados = valor.substring(0, valor.indexOf('.') - 2).toFloat();
  float minutos = valor.substring(valor.indexOf('.') - 2).toFloat();
  float decimal = grados + (minutos / 60.0);
  if (direccion == "S" || direccion == "W") {
    decimal *= -1;
  }
  return decimal;
}

void setup_wifi() {
  Serial.println("Conectando a WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi conectado");
  Serial.print("IP local: ");
  Serial.println(WiFi.localIP());
}

void reconnect_mqtt() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    if (client.connect("ESP32GPSClient")) {
      Serial.println(" Conectado a MQTT");
      client.publish(mqtt_topic, "Mensaje de prueba desde ESP32");
    } else {
      Serial.print("Error: ");
      Serial.print(client.state());
      delay(2000);
    }
  }
}

String leerCoordenadas() {
  while (A9G.available()) {
    String linea = A9G.readStringUntil('\n');
    linea.trim();

    if (linea.indexOf("$GNGGA") != -1) {
      Serial.println("Línea GNGGA recibida: " + linea);

      int campos[7];
      int index = 0;

      for (int i = 0; i < linea.length() && index < 7; i++) {
        if (linea.charAt(i) == ',') {
          campos[index++] = i;
        }
      }

      String latRaw = linea.substring(campos[1] + 1, campos[2]);
      String latDir = linea.substring(campos[2] + 1, campos[3]);
      String lonRaw = linea.substring(campos[3] + 1, campos[4]);
      String lonDir = linea.substring(campos[4] + 1, campos[5]);
      String fixQuality = linea.substring(campos[5] + 1, campos[6]);

      if (fixQuality != "0" && latRaw.length() > 0 && lonRaw.length() > 0) {
        float lat = convertirCoordenada(latRaw, latDir);
        float lon = convertirCoordenada(lonRaw, lonDir);

        String json = "{\"lat\": " + String(lat, 6) + ", \"lon\": " + String(lon, 6) + "}";
        return json;
      }
    }
  }
  return "";
}

void setup() {
  Serial.begin(115200);
  A9G.begin(115200, SERIAL_8N1, RXD2, TXD2);

  pinMode(botonPin, INPUT_PULLUP);

  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  reconnect_mqtt();

  A9G.println("AT+GPS=1");     
  delay(3000);
  A9G.println("AT+GPSRD=5");
}

void loop() {
  if (!client.connected()) {
    reconnect_mqtt();
  }
  client.loop();

  String datosGPS = leerCoordenadas();
  if (datosGPS != "") {
    Serial.println("Enviando: " + datosGPS);
    if (client.publish(mqtt_topic, datosGPS.c_str())) {
      Serial.println("Publicación MQTT exitosa");
    } else {
      Serial.println("Error al publicar MQTT");
    }
  }

  if (digitalRead(botonPin) == HIGH) {
    Serial.println("Botón de emergencia presionado");
    String mensajeEmergencia = "{\"alerta\": \"emergencia\", \"mensaje\": \"Botón presionado\"}";
    if (client.publish(mqtt_topic_emergencia, mensajeEmergencia.c_str())) {
      Serial.println("Emergencia enviada por MQTT");
    } else {
      Serial.println("Error al enviar emergencia");
    }
    delay(1000);
  }

  delay(6000);
}
