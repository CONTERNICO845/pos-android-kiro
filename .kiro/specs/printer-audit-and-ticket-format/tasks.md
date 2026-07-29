+ Implementation Plan: Printer Audit and Ticket Format

## Overview

Este plan implementa dos entregables: (1) un documento `printer_architecture.md` que describe el flujo completo de impresión, y (2) correcciones al formato de ticket para incluir información de pago en efectivo/cambio y extraNotes por producto. Las tareas se organizan de forma incremental: primero la documentación, luego los cambios al modelo de datos, después el formateo, y finalmente la integración con el ViewModel.

## Tasks

- [x] 1. Crear documento de arquitectura de impresión
  - [x] 1.1 Crear `printer_architecture.md` en la raíz del proyecto
    - Describir el flujo completo desde "Confirmar Pago" hasta bytes TCP: IP validation, folio generation, ticket text generation via TicketFormatter, y printDoubleTicket call
    - Describir el mapping de CartItem → TicketLineItem (quantity, productName, totalPrice, selectedCustomizations → optionName)
    - Describir el formato de texto con ancho fijo de 48 chars: CANT(5) + DESCRIPCION(30) + IMPORTE(13)
    - Describir diferencias entre ticket cliente (con precios, subtotal, IVA, total) y ticket interno (sin precios, solo conteo de artículos)
    - Describir la conexión TCP en puerto 9100 con connect timeout 5s y coroutine timeout 15s via `withTimeout`
    - Describir la secuencia ESC/POS: ESC_INIT (0x1B 0x40), payload Cp850, 3 LF, ESC_CUT (0x1D 0x56 0x00)
    - Describir el flujo double-ticket: cada ticket precedido por ESC_INIT y seguido de LF+ESC_CUT, enviados secuencialmente sobre una sola conexión TCP
    - Describir el error-handling: printAttempts incrementa en fallo, máximo 3 intentos, si todos fallan muestra error y re-habilita botón sin persistir orden
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [x] 2. Modificar TicketLineItem para incluir extraNotes
  - [x] 2.1 Agregar campo `extraNotes: String = ""` a la data class TicketLineItem
    - Agregar el parámetro con valor default vacío para mantener retrocompatibilidad
    - _Requirements: 3.1_

- [x] 3. Actualizar PosViewModel para pasar extraNotes y datos de pago
  - [x] 3.1 Modificar el mapping CartItem → TicketLineItem en `confirmPayment()`
    - Pasar `cartItem.extraNotes` al nuevo campo `extraNotes` de TicketLineItem
    - _Requirements: 3.2, 6.1, 6.6_

  - [x] 3.2 Calcular `change` y pasar `cashReceived` y `change` a `formatClientTicket`
    - Cuando paymentStatus es PAGADO y cashReceived > 0: calcular change = cashReceived - totalAmount usando BigDecimal HALF_UP, coerce a mínimo 0.0
    - Cuando cashReceived == 0 o paymentStatus != PAGADO: pasar 0.0 para ambos valores
    - _Requirements: 2.1, 2.4, 2.5, 2.6_

- [x] 4. Implementar cambios en TicketFormatter
  - [x] 4.1 Actualizar firma de `formatClientTicket` para aceptar `cashReceived` y `change`
    - Agregar parámetros `cashReceived: Double = 0.0` y `change: Double = 0.0` después de `totalAmount`
    - _Requirements: 2.2_

  - [x] 4.2 Implementar función privada `formatFinancialSection`
    - Renderizar Subtotal (antes de IVA), IVA (16%), Total en ese orden
    - Calcular subtotal = total / 1.16 y IVA = total - subtotal, ambos con BigDecimal HALF_UP scale 2
    - Cuando cashReceived > 0 y status es PAGADO: renderizar "Pago (Efectivo MXN):" y "Cambio:"
    - Cada línea debe ser exactamente 48 chars: label left-aligned + amount right-aligned
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [x] 4.3 Implementar función privada `formatExtraNotes`
    - Prefijo primera línea: `"      * Nota: "` (14 chars), deja 34 chars de contenido
    - Continuación: 13 espacios de indentación, 35 chars de contenido por línea
    - Máximo 8 líneas de continuación
    - Retornar string vacío si extraNotes está vacío o solo whitespace
    - _Requirements: 3.3, 3.4, 3.5_

  - [x] 4.4 Integrar renderizado de extraNotes y personalizar el orden en la lista de productos
    - Renderizar personalizaciones con prefijo `"      - "`, truncando optionName a 40 chars
    - Renderizar extraNotes después de personalizaciones con `"      * Nota: "`
    - Aplicar el mismo renderizado tanto en ticket cliente como en ticket interno
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 3.6_

  - [x] 4.5 Integrar `formatFinancialSection` en el flujo de `formatClientTicket`
    - Reemplazar la sección actual de totales con la nueva sección financiera completa
    - Incluir separadores (48 guiones) antes y después de la sección financiera
    - Omitir líneas de pago/cambio cuando cashReceived es 0.0
    - _Requirements: 4.1, 2.3, 2.5_

- [x] 5. Checkpoint - Verificar compilación y coherencia
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Property-Based Tests y Unit Tests
  - [x]* 6.1 Crear generadores Kotest para tests de propiedad
    - Crear `Arb.cartItem()`: quantity 1–99, prices > 0, extraNotes 0–280 chars, 0–5 customizations
    - Crear `Arb.ticketLineItem()`: derivado de cartItem generator
    - Crear `Arb.positiveMoney()`: Double positivo $0.01–$999,999.99
    - Crear `Arb.extraNotesString()`: String 0–300 chars con caracteres latinos válidos para Cp850
    - Crear `Arb.paymentScenario()`: tupla (cashReceived, totalAmount, paymentStatus)
    - Ubicación: `app/src/test/java/com/example/puntodeventa/ui/pos/`
    - _Requirements: 6.1, 6.6_

  - [x]* 6.2 Write property test: CartItem to TicketLineItem mapping preserves all fields
    - **Property 1: CartItem to TicketLineItem mapping preserves all fields**
    - **Validates: Requirements 6.1, 6.6, 3.2**
    - Archivo: `CartToTicketMappingPropertyTest.kt`

  - [x]* 6.3 Write property test: Subtotal + IVA = Total arithmetic invariant
    - **Property 2: Subtotal + IVA = Total arithmetic invariant**
    - **Validates: Requirements 4.9**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.4 Write property test: Payment section rendered when cash payment present
    - **Property 3: Payment section rendered when cash payment present**
    - **Validates: Requirements 2.3, 2.4, 4.6, 4.7**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.5 Write property test: Payment section omitted when cash payment absent
    - **Property 4: Payment section omitted when cash payment absent**
    - **Validates: Requirements 2.5, 4.8**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.6 Write property test: Financial section lines are exactly 48 characters and in correct order
    - **Property 5: Financial section lines are exactly 48 characters and in correct order**
    - **Validates: Requirements 4.1, 4.2**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.7 Write property test: ExtraNotes rendering with correct prefix and wrapping
    - **Property 6: ExtraNotes rendering with correct prefix and wrapping**
    - **Validates: Requirements 3.3, 3.5, 5.2, 5.3, 5.4**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.8 Write property test: Empty extraNotes produces no note line
    - **Property 7: Empty extraNotes produces no note line**
    - **Validates: Requirements 3.4, 5.5**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.9 Write property test: Customizations rendered with dash prefix before notes
    - **Property 8: Customizations rendered with dash prefix before notes**
    - **Validates: Requirements 5.1, 5.2, 6.4**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.10 Write property test: Every product name appears in formatted output
    - **Property 9: Every product name appears in formatted output**
    - **Validates: Requirements 6.2**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.11 Write property test: ExtraNotes appears consistently on both ticket types
    - **Property 10: ExtraNotes appears consistently on both ticket types**
    - **Validates: Requirements 3.6, 5.6**
    - Archivo: `TicketFormatterPropertyTest.kt`

  - [x]* 6.12 Write unit tests for TicketFormatter edge cases
    - Test labels exactos ("Subtotal (antes de IVA):", "IVA (16%):", "Total:", etc.)
    - Test cashReceived < totalAmount con PAGADO → cambio "$0.00"
    - Test ticket con 0 customizations y 0 extraNotes → sin líneas extra
    - Test extraNotes con exactamente 34 chars → no wrap
    - Test extraNotes con 35 chars → exactamente 1 línea de continuación
    - Test productName de 30 chars → no trunca; 31 chars → trunca a 30
    - Test optionName de 40 chars → no trunca; 41 chars → trunca a 40
    - Archivo: `TicketFormatterUnitTest.kt`
    - _Requirements: 2.6, 3.3, 3.4, 3.5, 4.1, 4.2, 5.1, 6.2_

- [x] 7. Final checkpoint - Verificar que todo compila y los tests pasan
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- El documento `printer_architecture.md` es un artefacto de documentación puro — no modifica código
- Los cambios de código son retrocompatibles gracias a parámetros con valores default

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["3.1", "4.1"] },
    { "id": 2, "tasks": ["3.2", "4.3"] },
    { "id": 3, "tasks": ["4.2", "4.4"] },
    { "id": 4, "tasks": ["4.5"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4", "6.5", "6.6", "6.7", "6.8", "6.9", "6.10", "6.11", "6.12"] }
  ]
}
```
