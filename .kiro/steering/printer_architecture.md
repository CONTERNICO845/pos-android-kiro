# Arquitectura de Impresión — Punto de Venta

Este documento describe el estado real del flujo multiimpresora LAN, desde la configuración y el descubrimiento hasta la impresión, reintentos y persistencia de una orden.

## 1. Modelo y persistencia

`PrinterConfig` es la configuración persistida de un destino ESC/POS y contiene exactamente 8 campos: `id: String`, `name: String`, `ipAddress: String`, `port: Int = 9100`, `paperSize: Int = 80`, `autoCut: Boolean = true`, `protocol: String = "ESC/POS"` e `isActive: Boolean = true`. El `id` es estable y permite actualizar, activar y rastrear cada impresora sin depender de su IP.

`PrinterPreferencesRepository` guarda la colección completa como JSON en un Preferences DataStore dedicado (`printer_preferences`, clave `printers_json`) mediante kotlinx.serialization, con defaults incluidos y tolerancia a campos desconocidos; esa lista JSON es la única fuente de verdad. En el primer acceso sin datos migra automáticamente desde el `SharedPreferences` legacy `printer_config`: prioriza `printers_json` si existe y es válido; si falta o está corrupto, usa `ip_address` o el fallback `192.168.1.248`, crea `PrinterConfig.default(...)` con ID `default-printer`, nombre `Impresora principal` y los demás defaults, y persiste inmediatamente la lista en DataStore. Un JSON corrupto ya almacenado en DataStore también se reemplaza por esa impresora inicial. Las APIs síncronas y las APIs legacy de IP se conservan por compatibilidad mediante I/O bloqueante en `Dispatchers.IO`, pero todas leen y escriben exclusivamente la colección de DataStore.

## 2. Pantalla de configuración multiimpresora

El botón **Impresoras** abre un `ModalBottomSheet` con las impresoras guardadas, su `IP:puerto`, selección para editar, un `Switch` de activa/inactiva y una acción **Eliminar** por destino, además de **Agregar Nueva Impresora**. Se pueden crear y eliminar múltiples impresoras; si se elimina la seleccionada se abre la siguiente disponible, y si la lista queda vacía se prepara un formulario nuevo. El formulario edita nombre, IPv4, puerto, papel 58/80 mm, autocorte y protocolo/modelo; permite buscar, seleccionar una IP descubierta, probar y guardar.

Un alta nueva usa: nombre `Nueva impresora`, IP vacía, puerto `9100`, papel `80`, autocorte activo, protocolo `ESC/POS` y estado activo. El guardado exige nombre, IPv4 válida, puerto 1..65535, papel 58 u 80 y protocolo ESC/POS. La prueba usa todos los valores del borrador; guardar hace upsert por `id`.

## 3. Descubrimiento LAN y permiso

En API 37 o superior, `PrinterScreen` solicita en runtime `android.permission.ACCESS_LOCAL_NETWORK`; en versiones anteriores inicia directamente. El manifest declara `INTERNET`, `ACCESS_NETWORK_STATE` y `ACCESS_LOCAL_NETWORK`. Si se deniega el permiso, se informa el error y no se escanea.

`LanPrinterDiscovery` toma la primera IPv4 privada activa (`10/8`, `172.16/12` o `192.168/16`) y escanea su subred `/24`: hosts `1..254`, omitiendo la IP local. Usa el puerto del borrador; su default es `9100`. Ejecuta como máximo 32 probes TCP concurrentes, con timeout de conexión de 250 ms, y devuelve las IP ordenadas numéricamente cuyo puerto aceptó la conexión. El resultado es solo una lista de puertos abiertos: no garantiza que el host sea una impresora ni que hable ESC/POS.

## 4. Complete Order: fan-out secuencial

`PosViewModel.confirmPayment()` obtiene `getPrinters()`, filtra exclusivamente `isActive == true` y bloquea el flujo con `No hay impresoras activas configuradas` si no queda ninguna; en ese caso no imprime ni persiste. Después genera un folio secuencial con padding a 3 dígitos, mapea el carrito a `TicketLineItem` y forma el ticket cliente, el ticket interno persistible y los segmentos del interno para doble altura.

Las impresoras activas se recorren **secuencialmente**. Para cada una, `EscPosPrinterLan.printOrder()` abre exactamente una conexión TCP propia y, sobre esa conexión, envía primero el ticket cliente y luego el interno (header normal, items en doble altura, footer normal), hace `flush` y cierra el socket. No se comparte una conexión entre impresoras.

Cada destino usa su `ipAddress`, `port`, `paperSize`, `autoCut` y `protocol`: solo se admite `ESC/POS`; 80 mm conserva el formato de 48 columnas y 58 mm adapta el texto en bloques de 32 columnas; el corte `GS V 0` se emite solo si `autoCut` está activo. Ambos tickets comienzan con `ESC @`, terminan con tres saltos de línea y se codifican en `Cp850`.

## 5. Reintentos y persistencia

`printedPrinterIds` registra el `id` inmediatamente después de que una impresora completa ambos tickets. Si una posterior falla, el intento termina, el carrito se conserva y el botón permite reintentar; el siguiente intento filtra esos IDs y envía solo a las impresoras todavía fallidas, evitando duplicar tickets en destinos exitosos. El límite continúa siendo 3 fallos manuales; el tracking se limpia al iniciar un checkout nuevo o al completar correctamente la orden.

La orden y sus textos se persisten **solo después de que todas las impresoras activas pendientes hayan terminado con éxito**. Una falla parcial o total de impresión no guarda la orden. Si la impresión completa pero falla Room, se informa `Error al guardar la orden` sin volver a imprimir automáticamente.

## 6. Reimpresión desde historial

`TicketHistoryViewModel.onReprintTicket()` vuelve a consultar la colección y envía el **ticket cliente** guardado a todas las impresoras activas, secuencialmente, usando `ipAddress`, `port`, `paperSize` y `autoCut` de cada una. Si no hay activas, bloquea con el mismo mensaje. Este flujo no imprime el ticket interno y no mantiene tracking de destinos exitosos entre reintentos de reimpresión.

## 7. Transporte, timeouts y bytes ESC/POS

Toda conexión de impresión usa `Dispatchers.IO`, connect timeout de 5.000 ms, `socket.soTimeout` de 5.000 ms y timeout global de coroutine de 15.000 ms. El socket se cierra en `finally`. El payload usa `Cp850`; los comandos actuales son `ESC @` (`1B 40`), `ESC ! 10`/`ESC ! 00` para doble altura/normal y, cuando corresponde, `GS V 0` (`1D 56 00`) para corte completo.

`TicketFormatter` permanece puro. En 80 mm mantiene 48 columnas (`CANT` 5 + `DESCRIPCION` 30 + `IMPORTE` 13), personalizaciones/notas y divisores; el ticket cliente incluye importes, IVA, efectivo/cambio y el interno omite importes. La adaptación a 32 columnas para papel de 58 mm ocurre en `EscPosPrinterLan`, no en el formatter.