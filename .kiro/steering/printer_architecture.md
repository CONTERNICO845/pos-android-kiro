# Arquitectura de Impresión — Punto de Venta

Este documento describe el flujo completo desde que el cajero presiona **"Confirmar Pago"** hasta que los bytes llegan a la impresora térmica POS-8360 por TCP/IP en la red local.

---

## 1. Flujo Completo: Confirmar Pago → Bytes TCP

Cuando el usuario presiona el botón de confirmar, se ejecuta `PosViewModel.confirmPayment()` con los siguientes pasos secuenciales:

### 1.1 Validación de IP

```
PosViewModel → PrinterPreferencesRepository.getIpAddress()
```

- La IP se obtiene de `SharedPreferences` (clave `"ip_address"`, default `"192.168.1.248"`).
- Si la IP retornada es vacía (`""`), el flujo se aborta inmediatamente con un mensaje de error: *"No se ha configurado la IP de la impresora"*.
- El botón se re-habilita y no se persiste ninguna orden.

### 1.2 Generación de Folio

```kotlin
val orderCount = orderRepository.getOrderCount()
val folio = (orderCount + 1).toString().padStart(3, '0')
```

- El folio es un número secuencial basado en el conteo actual de órdenes en la base de datos.
- Se formatea con zero-padding a 3 dígitos: `"001"`, `"002"`, ..., `"999"`.

### 1.3 Generación de Texto del Ticket

```
PosViewModel → TicketFormatter.formatClientTicket(folio, dateTime, customerName, paymentStatus, items, totalAmount)
PosViewModel → TicketFormatter.formatInternalTicket(folio, dateTime, customerName, paymentStatus, items)
```

- `TicketFormatter` es un object puro (sin side effects) que genera strings formateados.
- Se generan dos textos: uno para el **ticket cliente** (con precios) y otro para el **ticket interno** (sin precios).

### 1.4 Impresión vía TCP

```
PosViewModel → EscPosPrinterLan.printDoubleTicket(ipAddress, clientTicketText, internalTicketText)
```

- Envía ambos tickets sobre una única conexión TCP al puerto 9100.
- Si falla, se entra al flujo de reintentos (ver sección 7).

### 1.5 Persistencia (solo tras impresión exitosa)

- Solo si la impresión fue exitosa, se persiste la orden en la base de datos junto con los textos de ticket.
- Si la persistencia falla, se muestra un error pero no se reintenta la impresión.

---

## 2. Mapping de CartItem → TicketLineItem

El ViewModel transforma cada `CartItem` del carrito en un `TicketLineItem` para consumo del formatter:

| Campo CartItem | → | Campo TicketLineItem |
|----------------|---|---------------------|
| `quantity` | → | `quantity` |
| `productName` | → | `productName` |
| `totalPrice` | → | `lineTotal` |
| `selectedCustomizations[].optionName` | → | `customizations: List<String>` |

### Código de mapping actual

```kotlin
val ticketLineItems = cartItems.map { item ->
    TicketLineItem(
        quantity = item.quantity,
        productName = item.productName,
        lineTotal = item.totalPrice,
        customizations = item.selectedCustomizations.map { it.optionName }
    )
}
```

**Notas:**
- Se produce exactamente un `TicketLineItem` por cada `CartItem`.
- El orden de la lista se preserva.
- Las personalizaciones se mapean a sus `optionName` en el mismo orden.

---

## 3. Formato de Texto — Ancho Fijo 48 Caracteres

`TicketFormatter` genera texto plano con ancho fijo de **48 caracteres** por línea, compatible con impresoras térmicas de 80mm.

### Layout de columnas para líneas de producto

```
|CANT |DESCRIPCION                   |      IMPORTE|
|5ch  |30ch                          |     13ch    |
```

| Columna | Ancho | Alineación | Contenido |
|---------|-------|------------|-----------|
| CANT | 5 chars | Izquierda (padEnd) | Cantidad del artículo |
| DESCRIPCION | 30 chars | Izquierda (padEnd) | Nombre del producto (truncado a 30 chars) |
| IMPORTE | 13 chars | Derecha (padStart) | Precio formateado como `$X.XX` |

### Ejemplo de línea formateada

```
1    Taco al Pastor                      $45.00
```

### Separadores

Se usa una línea de 48 guiones (`-`) como separador visual:
```
------------------------------------------------
```

---

## 4. Diferencias entre Ticket Cliente y Ticket Interno

### Ticket Cliente (para el comprador)

Incluye:
- Header: nombre del negocio, folio, fecha/hora, nombre del cliente, status de pago
- Lista de productos **con precios** por línea
- Personalizaciones listadas debajo de cada producto
- Sección financiera: Subtotal, IVA (16%), Total
- Footer: mensaje de agradecimiento

### Ticket Interno (para cocina)

Incluye:
- Header: idéntico al ticket cliente
- Lista de productos **sin precios** (columna IMPORTE vacía)
- Personalizaciones listadas debajo de cada producto
- Línea de conteo total: `"Total: N Artículos"` (suma de quantities)
- Footer: mensaje de agradecimiento

**No incluye:** subtotal, IVA, total, ni importes por línea.

### Cálculos financieros (solo ticket cliente)

```kotlin
Subtotal = Total / 1.16    (BigDecimal, HALF_UP, scale 2)
IVA      = Total - Subtotal (garantiza Subtotal + IVA = Total exacto)
```

---

## 5. Conexión TCP — EscPosPrinterLan

`EscPosPrinterLan` es un `object` (singleton) que gestiona la comunicación con la impresora térmica.

### Parámetros de conexión

| Parámetro | Valor |
|-----------|-------|
| Puerto | 9100 (estándar ESC/POS) |
| Connect timeout | 5,000 ms (5 segundos) |
| Socket read timeout (`soTimeout`) | 5,000 ms |
| Coroutine timeout (`withTimeout`) | 15,000 ms (15 segundos) |
| Dispatcher | `Dispatchers.IO` |
| Charset | `Cp850` (Code Page 850 — caracteres latinos) |

### Estructura de la conexión

```kotlin
withTimeout(15_000L) {
    withContext(Dispatchers.IO) {
        val socket = Socket()
        socket.connect(InetSocketAddress(ipAddress, 9100), 5_000)
        socket.soTimeout = 5_000
        // ... envío de bytes ...
        socket.close()
    }
}
```

- `withTimeout` envuelve toda la operación (conexión + envío) para proteger contra bloqueos indefinidos.
- `withContext(Dispatchers.IO)` ejecuta las operaciones de socket en el pool de I/O.
- El socket se cierra en un bloque `finally` con `runCatching` para ignorar errores de cierre.

---

## 6. Secuencia ESC/POS

Cada ticket individual se envía con la siguiente secuencia de bytes:

```
┌──────────────┬─────────────────────────────────────┬───────────┬──────────────┐
│  ESC_INIT    │          Payload (texto)            │   3 × LF  │   ESC_CUT    │
│ 0x1B 0x40    │  ticketText.toByteArray("Cp850")   │ "\n\n\n"  │ 0x1D 0x56 00 │
└──────────────┴─────────────────────────────────────┴───────────┴──────────────┘
```

### Comandos ESC/POS utilizados

| Comando | Bytes | Función |
|---------|-------|---------|
| ESC_INIT | `0x1B 0x40` | Inicializa/resetea la impresora a su estado default |
| Line Feed | `0x0A` (×3) | Avanza el papel 3 líneas para separar del corte |
| ESC_CUT | `0x1D 0x56 0x00` | Ejecuta un corte completo del papel |

### Encoding Cp850

- Code Page 850 es un charset de 8 bits compatible con caracteres latinos (tildes, eñes).
- Permite imprimir correctamente texto en español sin problemas de encoding.

---

## 7. Flujo Double-Ticket

`printDoubleTicket` envía ambos tickets (cliente + interno) sobre **una sola conexión TCP**:

```
Conexión TCP (puerto 9100)
│
├─ Ticket 1 (Cliente):
│   ESC_INIT → payload Cp850 → "\n\n\n" → ESC_CUT
│
├─ Ticket 2 (Interno):
│   ESC_INIT → payload Cp850 → "\n\n\n" → ESC_CUT
│
└─ flush() → close()
```

### Secuencia detallada

1. Abrir socket TCP al puerto 9100 con connect timeout de 5s
2. **Ticket cliente:**
   - Escribir `ESC_INIT` (0x1B 0x40)
   - Escribir `clientTicketText` codificado en Cp850
   - Escribir 3 line feeds (`"\n\n\n"`)
   - Escribir `ESC_CUT` (0x1D 0x56 0x00)
3. **Ticket interno:**
   - Escribir `ESC_INIT` (0x1B 0x40)
   - Escribir `internalTicketText` codificado en Cp850
   - Escribir 3 line feeds (`"\n\n\n"`)
   - Escribir `ESC_CUT` (0x1D 0x56 0x00)
4. Flush del output stream
5. Cerrar socket (en bloque finally)

**Ventaja:** Una sola conexión TCP reduce la latencia total y evita problemas de conexiones concurrentes con la impresora.

---

## 8. Error Handling — Reintentos de Impresión

### Flujo de reintentos

```
confirmPayment() llamado
    ├─ Intento 1: printDoubleTicket() → Exception
    │   └─ printAttempts = 1, botón = "Reintentar", error mostrado
    ├─ Intento 2: printDoubleTicket() → Exception
    │   └─ printAttempts = 2, botón = "Reintentar", error mostrado
    └─ Intento 3: printDoubleTicket() → Exception
        └─ printAttempts = 3, botón = "Confirmar Pago", error final mostrado
```

### Comportamiento por intento

| Intento | Falla | Acción |
|---------|-------|--------|
| 1 | Sí | `printAttempts = 1`, texto del botón → "Reintentar", muestra error con `e.message` |
| 2 | Sí | `printAttempts = 2`, texto del botón → "Reintentar", muestra error con `e.message` |
| 3 | Sí | `printAttempts = 3`, texto del botón → "Confirmar Pago", muestra "No se pudo imprimir después de 3 intentos" |

### Reglas de error handling

- **Máximo 3 intentos** antes de mostrar el error final.
- **La orden NO se persiste** si la impresión falla (no hay print = no hay orden guardada).
- **El botón se re-habilita** (`isPrinting = false`) para permitir reintentos manuales.
- **El carrito se mantiene intacto** — el cajero no pierde su trabajo.
- **El campo `printAttempts`** se incrementa en cada fallo y se resetea al iniciar un nuevo checkout.
- Si después de 3 fallos el cajero quiere reintentar, debe cerrar/abrir el checkout para resetear los intentos.

### Tipos de error capturados

Cualquier `Exception` lanzada por `printDoubleTicket` activa el flujo de reintentos. Esto incluye:
- `java.net.ConnectException` — impresora apagada o IP incorrecta
- `java.net.SocketTimeoutException` — timeout de conexión (5s) o de coroutine (15s)
- `java.io.IOException` — error de escritura en el socket
- `kotlinx.coroutines.TimeoutCancellationException` — timeout general de la coroutine (15s)

---

## Diagrama de Secuencia Completo

```
┌─────────┐    ┌────────────┐    ┌─────────┐    ┌───────────┐    ┌──────────────────┐    ┌─────────┐
│   UI    │    │PosViewModel│    │PrinterPR │    │TicketFmt  │    │EscPosPrinterLan  │    │Printer  │
└────┬────┘    └─────┬──────┘    └────┬─────┘    └─────┬─────┘    └────────┬─────────┘    └────┬────┘
     │               │                │                 │                    │                   │
     │ confirmPay()  │                │                 │                    │                   │
     │──────────────>│                │                 │                    │                   │
     │               │ getIpAddress() │                 │                    │                   │
     │               │───────────────>│                 │                    │                   │
     │               │    "192.168.." │                 │                    │                   │
     │               │<───────────────│                 │                    │                   │
     │               │                                  │                    │                   │
     │               │ map CartItem[] → TicketLineItem[]│                    │                   │
     │               │──────────────────────────────────│                    │                   │
     │               │                                  │                    │                   │
     │               │ formatClientTicket(...)           │                    │                   │
     │               │─────────────────────────────────>│                    │                   │
     │               │               clientText: String │                    │                   │
     │               │<─────────────────────────────────│                    │                   │
     │               │                                  │                    │                   │
     │               │ formatInternalTicket(...)         │                    │                   │
     │               │─────────────────────────────────>│                    │                   │
     │               │             internalText: String  │                    │                   │
     │               │<─────────────────────────────────│                    │                   │
     │               │                                                       │                   │
     │               │ printDoubleTicket(ip, client, internal)               │                   │
     │               │──────────────────────────────────────────────────────>│                   │
     │               │                                                       │  TCP:9100 bytes   │
     │               │                                                       │──────────────────>│
     │               │                                                       │       ACK         │
     │               │                                    success            │<──────────────────│
     │               │<──────────────────────────────────────────────────────│                   │
     │               │                                                                           │
     │               │ persistOrder(...)                                                         │
     │               │──────(DB)──────                                                           │
     │               │                                                                           │
     │  resetState   │                                                                           │
     │<──────────────│                                                                           │
```
