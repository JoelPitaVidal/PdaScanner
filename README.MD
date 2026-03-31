# PDA Scanner 📦📸

**PDA Scanner** es una solución industrial nativa para Android diseñada para optimizar el flujo de registro de inventario en almacenes. La aplicación permite la vinculación rápida entre documentos de transporte y evidencias gráficas mediante el escaneo de códigos QR y la captura fotográfica automatizada.

---

## 🚀 Flujo de Trabajo (User Experience)

La aplicación está diseñada para ser utilizada con una sola mano, aprovechando los componentes físicos de una PDA industrial:

1.  **Escaneo Activo**: El operario apunta al código QR. La cámara detecta automáticamente si es un Albarán de Transporte (`AT`) o de Cliente (`AC`).
2.  **Validación Inteligente**:
    *   Si es `AT`, el sistema consulta la base de datos local (**Room**) para encontrar el cliente asociado.
    *   Si es `AC`, se marca como listo directamente.
3.  **Confirmación Hápitca**: La PDA vibra para indicar que el código es válido y la interfaz cambia a **Verde**.
4.  **Disparo Físico**: El operario presiona cualquier gatillo lateral o el botón central de la PDA para capturar la foto.
5.  **Sincronización**: La imagen se guarda en la galería local, se registra en la DB interna y se envía mediante un proceso en segundo plano a la **API central**.

---

## 🛠 Tecnologías y Librerías

| Tecnología | Propósito |
| :--- | :--- |
| **Kotlin + Coroutines** | Lenguaje principal y gestión de hilos asíncronos. |
| **CameraX** | API de cámara de alta eficiencia compatible con hardware rugerizado. |
| **Google ML Kit** | Motor de reconocimiento de códigos de barras y QR. |
| **Room Persistence** | Base de datos SQLite local para funcionamiento Offline-First. |
| **Retrofit 2 & OkHttp** | Cliente HTTP para la sincronización con el servidor central. |
| **ViewBinding** | Acceso seguro y eficiente a los componentes de la interfaz. |
| **MVVM Architecture** | Separación clara entre la lógica de negocio y la interfaz de usuario. |

---

## 📂 Estructura del Proyecto

El código sigue una estructura de paquetes por funcionalidad para facilitar el mantenimiento:

```text
com.example.pdascanner
├── api/                    # Comunicación con el servidor (Retrofit)
│   ├── service/            # Definición de Endpoints
│   └── RetrofitClient      # Configuración del cliente y Base URL
├── barcodeanalyzer/        # Lógica de procesamiento de imágenes de ML Kit
├── cameraManager/          # Gestión de ciclo de vida de cámara y captura
├── localdatabase/          # Capa de persistencia local (Room)
│   ├── appdatabase/        # Configuración de la DB
│   ├── repository/         # Mediador entre DB local y API
│   └── entities/           # Modelos: Albaran.kt y Foto.kt
├── permissionmanager/      # Helper para gestión de permisos de cámara y storage
├── ui/                     # Capa de presentación
│   └── viewmodel/          # Estado de la UI y lógica de validación
└── MainActivity            # Punto de entrada y gestión de eventos de hardware-
```
## ⚙️ Adaptaciones para Hardware PDA

Este proyecto incluye correcciones y optimizaciones específicas para dispositivos rugerizados (Zebra, Honeywell, Datalogic, etc.) que garantizan un rendimiento industrial:

*   **Inversión de Sensor**: Implementación de una corrección de **180°** en el `PreviewView`. Esto soluciona el problema común en terminales industriales donde los sensores de cámara están montados de forma invertida respecto al chasis.
*   **Mapeo de Gatillos Físicos**: Intercepción de `KeyCodes` específicos para permitir el disparo de la cámara mediante los botones físicos del dispositivo:
    *   `131`: Gatill
    *   `KeyEvent.KEYCODE_ENTER`: Gatillo central o empuñadura de pistola.
*   **Ahorro de Energía**: El ciclo de vida de la cámara está estrictamente ligado al de la `MainActivity` mediante *Lifecycle-Aware components*, liberando el hardware de captura inmediatamente cuando la app pasa a segundo plano para maximizar la autonomía de la batería.

---

## 🛡️ Gestión de Estados de la UI

La aplicación utiliza un sistema de **estados reactivos** (`ScanState`) gestionados desde el ViewModel para guiar al operario visualmente sin necesidad de leer texto denso:

| Estado | Significado | Color de Fondo |
| :--- | :--- | :--- |
| **BUSCANDO** | Validando el código QR en la base de datos local. | 🔵 Azul |
| **VÁLIDO** | Código reconocido. El sistema está listo para la foto. | 🟢 Verde |
| **PROCESANDO** | Capturando la imagen y comprimiendo el archivo. | 🟡 Amarillo |
| **ERROR** | Código no válido, no encontrado o fallo de hardware. | 🔴 Rojo |
| **GUARDADO** | Confirmación de guardado local y envío a la cola de sincronización. | 🟢 Verde Brillante |

---

## 📡 Sincronización con el Servidor

La comunicación con el backend se realiza mediante peticiones **MultipartBody** a través de Retrofit. El servidor destino debe estar configurado para recibir los siguientes parámetros:

*   `foto`: El archivo binario (JPEG) de la captura fotográfica.
*   `qr`: String con el contenido del código QR validado (Albarán).
*   `nombre`: String con el identificador único de archivo generado por la app (QR + Timestamp).

> [!IMPORTANT]
> **Nota de red**: Para pruebas en entornos de desarrollo o red local, asegúrese de configurar la IP correcta en `RetrofitClient.kt`. Si el servidor no utiliza HTTPS, es obligatorio habilitar `android:usesCleartextTraffic="true"` en la etiqueta `<application>` del archivo `AndroidManifest.xml`.
