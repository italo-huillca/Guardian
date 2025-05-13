#include <WiFi.h>
#include <PubSubClient.h>

#define RXD2 16 
#define TXD2 17  

HardwareSerial A9G(2);  // UART2

const char* ssid = "Comunidad Innovadores";
const char* password = "INn0V4-2K23!*";

const char* mqtt_server = "172.22.135.187";
const int mqtt_port = 1883;
const char* mqtt_topic = "gps/ubicacion";

WiFiClient espClient;
PubSubClient client(espClient);

// convertir una coordenada GPS en formato grados/minutos a decimal.
// ajusta el signo según hemisferio (S/W negativo).

float convertirCoordenada(String valor, String direccion) {
  float grados = valor.substring(0, valor.indexOf('.') - 2).toFloat();
  float minutos = valor.substring(valor.indexOf('.') - 2).toFloat();
  float decimal = grados + (minutos / 60.0);
  if (direccion == "S" || direccion == "W") {
    decimal *= -1;
  }
  return decimal;
}

// para conectar WiFi 

void setup_wifi() {
  Serial.println("🔌 Conectando a WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n✓ WiFi conectado");
  Serial.print("IP local: ");
  Serial.println(WiFi.localIP());
}

// reconectar al servidor MQTT en caso de desconexión.

void reconnect_mqtt() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    if (client.connect("ESP32GPSClient")) {
      Serial.println(" Conectado a MQTT");
      client.publish(mqtt_topic, "🚀 Mensaje de prueba desde ESP32");
    } else {
      Serial.print("❌ Error: ");
      Serial.print(client.state());
      delay(2000);
    }
  }
}

//lee datos GPS desde el A9G busca la línea GNGGA,
//extrae lat/lon, los convierte a decimal y los devuelve en formato JSON.
//retorna una cadena vacía si no hay fix

String leerCoordenadas() {
  while (A9G.available()) {
    String linea = A9G.readStringUntil('\n');
    linea.trim();

    if (linea.indexOf("$GNGGA") != -1) {
      Serial.println("🛰️ Línea GNGGA recibida: " + linea);

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
      } else {
        Serial.println("⚠️ Sin fix GPS aún...");
      }
    } else {
      Serial.println("📡 Dato crudo recibido (ignorando): " + linea);
    }
  }
  return "";
}

// puerto serie, wiFi, MQTT y GPS del A9G.

void setup() {
  Serial.begin(115200);
  A9G.begin(115200, SERIAL_8N1, RXD2, TXD2);

  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  reconnect_mqtt();

  A9G.println("AT+GPS=1");     
  delay(3000);
  A9G.println("AT+GPSRD=5");    // cada 5 segundos
}

// conexión MQTT y enviar coordenadas

void loop() {
  if (!client.connected()) {
    reconnect_mqtt();
  }
  client.loop();

  String datosGPS = leerCoordenadas();
  if (datosGPS != "") {
    Serial.println("📤 Enviando: " + datosGPS);
    if (client.publish(mqtt_topic, datosGPS.c_str())) {
      Serial.println("✅ Publicación MQTT exitosa");
    } else {
      Serial.println("❌ Error al publicar MQTT");
    }
  }

  delay(6000);  // 6 segundos nueva lectura GPS
}
