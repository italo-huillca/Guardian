# 🎒 Guardian - App Móvil de Seguridad Infantil con GPS

## 🎯 Objetivo

En el Perú, más de 12,468 menores desaparecen cada año.  
**Guardian** es una **aplicación móvil Android** que forma parte de una solución inteligente para padres de familia, al integrarse con una **mochila GPS con botón de emergencia**.  
Permite visualizar la ubicación del niño en tiempo real, recibir alertas de emergencia y monitorear su seguridad escolar.

---

## ✅ To-Do por Etapas del Proyecto

### 🟢 Etapa 1: Login y Registro (✅ completo)
- Firebase Authentication
- Pantallas: Splash, Login, Registro
- Navegación segura con Jetpack Compose

### 🟡 Etapa 2: Mapa y Estado del Dispositivo (en progreso)
- Google Maps con ubicación del niño
- Estado del dispositivo (batería y red)
- Botón flotante para acciones rápidas

### 🟠 Etapa 3: Ubicación en Tiempo Real
- Foreground Service para envío de ubicación cada 5–10 segundos
- Almacenamiento en Firebase Realtime Database

### 🟣 Etapa 4: Historial de Rutas
- Firestore con historial por día
- Visualización en mapa y listado de lugares visitados

### 🔵 Etapa 5: Zonas Seguras (Geofencing)
- Crear geocercas y detectar salidas
- Notificaciones de alerta para los padres

### 🔴 Etapa 6: Botón de Emergencia
- Lectura GPIO desde ESP32
- Alerta instantánea vía Firebase Cloud Messaging (FCM)

### ⚫ Etapa 7: Optimización y Funciones Avanzadas
- Ahorro de batería
- Reconexión automática
- Exportación de rutas (PDF, Excel)

---

## 🧱 Estructura del Proyecto

- **data/**  
  Repositorios, servicios Firebase, fuentes de datos y lógica de red/local.

- **domain/**  
  Casos de uso, modelos de lógica empresarial, independientes del framework Android.

- **presentation/**  
  Interfaz gráfica con Jetpack Compose, organizada por pantallas:
  - `screen/`: Login, Registro, Mapa, Historial, Emergencia, etc.
  - `navigation/`: Configuración de rutas.
  - `component/`: Reutilizables UI (botones, inputs, headers).
  - `theme/`: Colores, tipografía y estilos.

- **MainActivity.kt**  
  Entrada principal de la app.

---

## 🛠️ Tecnologías y Herramientas

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Navegación:** Navigation Compose
- **Base de datos:** Firebase Realtime Database, Firestore
- **Autenticación:** Firebase Authentication
- **Mapas y ubicación:** Google Maps SDK, FusedLocationProviderClient
- **Servicios:** Foreground Service, Geofencing
- **Notificaciones:** Firebase Cloud Messaging (FCM)
- **Hardware externo:** ESP32 + GPS A9G + botón físico
- **Control de versiones:** Git + GitHub

---
