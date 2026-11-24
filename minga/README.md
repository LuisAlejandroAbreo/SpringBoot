# Minga - Banco de tiempo vecinal

Proyecto Spring Boot (Java 21, Spring Data JPA, Spring Security, MySQL) donde los vecinos
intercambian **horas de ayuda** en lugar de dinero: **1 hora = 1 hora**, sin importar el oficio.

## Cómo ejecutarlo en VS Code

### Requisitos
- **JDK 21** (`java -version` debe decir 21).
- VS Code con las extensiones **Extension Pack for Java** y **Spring Boot Extension Pack**
  (VS Code te las sugiere al abrir la carpeta). No necesitas instalar Maven: la extensión trae el suyo.

### Opción A: sin MySQL (la más rápida)
1. Abre la carpeta `minga` en VS Code (`File > Open Folder`).
2. Ve a **Run and Debug** (Ctrl+Shift+D), elige **"Minga (H2, sin MySQL)"** y pulsa F5.
3. Abre http://localhost:8080 (los datos quedan guardados en la carpeta `data/`).

### Opción B: con MySQL (local o en la nube, como Aiven) usando `.env`
La conexión **no** está escrita en `application.properties`: se lee del archivo `.env`.
1. Si no existe, copia `.env.example` como `.env`.
2. Abre `.env` y completa tus datos (host, puerto, base, usuario y `DB_PASSWORD`).
   El `.env` que viene en el zip ya trae los datos de Aiven: solo falta pegar la clave
   (en Aiven: Password > icono del ojo).
3. Elige **"Minga (MySQL, lee .env)"** y pulsa F5. Las tablas se crean solas.

Para MySQL local usa las líneas comentadas de `.env.example` (`DB_CREATE_IF_MISSING=true` crea la base).
Las variables reales del sistema operativo tienen prioridad sobre `.env` (útil en un servidor).

### Usuarios de ejemplo
Con `app.demo-data=true` (valor por defecto) la primera vez se cargan categorías, servicios y un
intercambio ya realizado. Clave de todos: `Minga#2026`.

| Usuario | Rol | Para qué |
|---|---|---|
| `admin` | ADMIN | crea categorías en el Panel |
| `ana`, `luis`, `marta` | USER | probar el flujo completo |

> Para producción, pon `app.demo-data=false` y cambia la clave del administrador.

## Qué hace
- **La Plaza**: ofertas ("puedo...") y necesidades ("necesito...") con filtros por tipo, categoría, barrio y texto.
- **Publicar**: crear, editar y pausar tus publicaciones.
- **Intercambios**: SOLICITADO, ACEPTADO, REALIZADO, RECHAZADO, CANCELADO o EXPIRADO. Las horas se mueven
  solo cuando **ambos confirman**.
- **Libro de horas**: saldo y movimientos. Es de solo inserción: no se edita ni se borra nada.
- **Perfil**: promedio de reseñas e insignias (Primera minga, Mano firme, Vecino estrella, Constante).
- **Panel de la Minga**: vecinos, intercambios, horas circuladas, categorías más pedidas y barrios.
- **Seguridad**: login, registro con validación de contraseña, bloqueo de 5 minutos tras 3 intentos fallidos,
  BCrypt, CSRF, sesión de 30 minutos y roles USER/ADMIN.
- **Reglas**: 3 horas de bienvenida, máximo 8 horas por trato, no se puede pedir más de lo que se tiene,
  no se puede pedir el propio servicio, reseña solo tras un trato realizado (una por persona),
  y las solicitudes sin respuesta expiran a los 7 días (tarea programada cada hora).

## Modelo de datos (7 entidades)
`Usuario`, `Categoria`, `Servicio`, `Intercambio`, `MovimientoHoras`, `Resena` y `Notificacion` (la campana).

- Un servicio pertenece a un usuario y a una categoría.
- Un intercambio guarda **proveedor** (da la ayuda) y **beneficiario** (la recibe) y lleva `@Version`
  para evitar dobles confirmaciones simultáneas.
- Cada trato realizado genera 2 movimientos (DEBITO al beneficiario y CREDITO al proveedor) en **una sola
  transacción**; las dos cuentas se bloquean en orden para que nadie gaste las mismas horas dos veces.

## Validaciones de agenda y publicaciones
- **Sin publicaciones repetidas**: una persona no puede publicar dos veces lo mismo (mismo tipo y mismo título,
  sin importar tildes, mayúsculas ni signos). Se valida en el servicio y la base de datos lo respalda con una
  restricción única.
- **Un servicio, un trato por día**: nadie puede agendar el mismo servicio para un día que ya tiene una solicitud
  o un trato aceptado. El día del encuentro es obligatorio, y la pantalla muestra y bloquea los días ocupados.
  El servicio se bloquea mientras se agenda, así dos solicitudes simultáneas se atienden una por una.
- **Mejor horario**: al publicar se elige mañana (6 a. m. – 12 m.), tarde (12 m. – 6 p. m.), noche (6 – 10 p. m.)
  o cualquier horario. La Plaza permite filtrar por horario.

## Reseñas y satisfacción de ambas partes
- En **Intercambios > Historial** cada trato realizado muestra tu reseña y la que la otra persona te dejó.
- En el **Perfil**: promedio, desglose de 1 a 5 estrellas y promedio según el rol (cuando ayudó y cuando recibió ayuda).
- En la **Plaza**, cada publicación muestra la calificación de quien la publica.

## Campana de notificaciones
Avisa cuando: alguien alza la mano en tu servicio, aceptan o rechazan tu solicitud, cancelan un trato,
la otra persona confirma (falta tu confirmación), el trato se realiza, recibes una reseña o tu solicitud expira.
El numerito rojo se actualiza solo cada 30 segundos; al pulsar un aviso se marca como leído y te lleva a la pantalla.

## Estructura
```
src/main/java/com/minga
  model/        entidades y enums
  repository/   Spring Data JPA (el libro de horas solo expone insertar y consultar)
  service/      reglas de negocio (IntercambioService, LibroHorasService, ...)
  controller/   API REST
  dto/          cuerpos JSON de entrada y salida (nunca se exponen las entidades)
  security/     Spring Security (login, CSRF, bloqueo por intentos)
  config/       datos de ejemplo y tarea programada
src/main/resources/static   pantallas HTML, CSS y JavaScript puro
```

## API (resumen)
| Método y ruta | Función |
|---|---|
| `POST /api/auth/register` | crear usuario y acreditar la bienvenida |
| `GET /api/servicios?tipo=&categoriaId=&franja=&barrio=&q=` | la Plaza con filtros |
| `GET /api/servicios/mios`, `POST`, `PUT /{id}`, `POST /{id}/pausar`, `POST /{id}/reanudar` | mis publicaciones |
| `POST /api/servicios/{id}/solicitar` | alzar la mano |
| `GET /api/servicios/{id}/ocupados` | días ya tomados de un servicio |
| `GET /api/intercambios/mios` | mis tratos (con las reseñas de ambas partes) |
| `GET /api/notificaciones`, `GET /api/notificaciones/conteo`, `POST /api/notificaciones/{id}/leer`, `POST /api/notificaciones/leer-todas` | campana |
| `POST /api/intercambios/{id}/aceptar`, `rechazar`, `cancelar`, `confirmar`, `resena` | cambios de estado y reseña |
| `GET /api/me/saldo`, `GET /api/me/movimientos`, `PUT /api/me/perfil` | libro de horas y perfil |
| `GET /api/usuarios/{username}/perfil` | perfil público |
| `GET /api/comunidad/impacto` | cifras del panel |
| `GET /api/categorias`, `POST /api/categorias` (ADMIN) | categorías |

## Pruebas
`mvn test` (o el panel Testing de VS Code): validador de contraseñas, reglas de insignias, estados y detección de títulos repetidos.

## Si no arranca
1. `java -version` debe ser 21.
2. Con MySQL: que esté encendido y la clave sea correcta (error típico: `Access denied` o `Communications link failure`).
   Prueba primero la opción A (H2) para descartar problemas de MySQL.
3. Puerto 8080 ocupado: cambia `server.port` en `application.properties`.
4. Ctrl+Shift+P > "Java: Clean Java Language Server Workspace" y luego "Maven: Reload project".

## Pendiente (extras de la propuesta)
Sección "Para ti" (sugerencias), crédito de hasta 2 horas y medias horas.

## Subir a GitHub sin problemas
- `.gitignore` ya excluye `.env`, certificados y llaves (`*.pem`, `*.key`, `*.jks`), `target/`, la base H2 (`data/`),
  registros e IDE. `.env.example` sí se sube porque no lleva claves reales.
- Antes del primer `git add .`, comprueba con `git status` que **no** aparezca `.env`.
- Si alguna vez subiste una clave a GitHub, cámbiala (Aiven: botón de restablecer junto a Password): borrarla del
  código no la quita del historial.

> Si ya habías ejecutado una versión anterior con MySQL/Aiven, `ddl-auto=update` agrega las columnas y la tabla nuevas sin borrar datos.
