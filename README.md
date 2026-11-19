# Microservicio Sakura Personas - Sakura Salón

## Descripción del proyecto
El microservicio de Personas de Sakura permite registrar y autenticar a los usuarios del salón de belleza, administrando el inicio de sesión, el perfil y el estado de clientes y administradores

## Integrantes
- Dairys Bernal
- Rocio Bustos
- Amaru Burdiles

## Microservicios relacionados
- Gateway

## Rutas principales
- **Registro e inicio de sesión:** `POST: /api/sakura/v1/personas/auth`
- **Gestionar usuarios:** `GET / POST / PUT: /api/sakura/v1/personas/usuario`

## Documentación Swagger
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI:** http://localhost:8080/v3/api-docs

## PRUEBA EN SWAGGER

Esta guía recorre, de punta a punta, el alta de un cliente, la administración de usuarios y el alta de un profesional. Hay exactamente tres casos. Hay que hacerlos en orden, porque el caso 2 usa el `id` del cliente creado en el caso 1, y el caso 3 usa el token del administrador.

Sirve para alguien que abre Swagger por primera vez. Cada paso indica la pantalla, el método, la ruta, el encabezado, el cuerpo listo para pegar y el resultado que debe verse.

## Servicios que deben estar arriba

| Servicio | Dirección | Swagger | Cuándo hace falta |
| --- | --- | --- | --- |
| Personas | http://localhost:8081 | http://localhost:8081/doc/swagger-ui.html | Casos 1, 2 y 3 |
| Catálogo | http://localhost:8082 | — | Caso 3, al leer o asociar servicios |
| Agenda | http://localhost:8084 | — | Caso 3, solo en el historial y en el DELETE |

Personas usa MySQL en `localhost:3306`, base `sakura_personas`, usuario `root` sin contraseña. El administrador de semilla ya existe: correo `admin@sakura.cl`, clave `Sakura1&`, rol `ADMIN`.

En personal, la semilla trae profesionales con id del 1 al 6 y turnos de lunes a viernes, de 09:00 a 18:00. El sábado (día 6) queda libre. El catálogo trae servicios con id del 1 al 12. El día de la semana se numera así: 1 es lunes y 7 es domingo.

## Cómo ejecutar un paso en Swagger

1. Abra la URL de Swagger del servicio que indica el paso.
2. Busque la operación por el método y la ruta. Ábrala.
3. Pulse **Try it out**.
4. Si el paso pide un encabezado, escriba el valor en el parámetro `Authorization` de esa operación. El valor es `Bearer`, un espacio y el token. También se acepta el token solo, sin la palabra Bearer.
5. Si la ruta tiene `{id}` o `{token}`, reemplace ese dato en el campo del parámetro, no dentro del JSON.
6. Si hay cuerpo, borre el ejemplo de Swagger y pegue el JSON del paso. El cuerpo debe ser JSON válido.
7. Pulse **Execute**.
8. Mire el código HTTP y el cuerpo de la respuesta. Anote en un bloc de notas los valores que el paso pide guardar (`id`, `token`). Un login nuevo reemplaza el token anterior de ese usuario.

Los correos de estos casos son distintos a los de la semilla. En personas el correo es único: si `ana.soto@ejemplo.cl` o `lucia.admin@ejemplo.cl` ya se registraron, el alta responde 409 con `El correo ya está registrado`. Cambie la parte local del correo, por ejemplo `ana.soto2@ejemplo.cl`, y use el mismo valor en todos los pasos de esa persona.

---

## Caso 1 — Cliente en personas

### Objetivo

Comprobar que una persona puede registrarse como cliente, iniciar sesión, consultar esa sesión, verificar su clave, actualizar el perfil, cambiar la clave, entrar de nuevo con la clave nueva y cerrar la sesión. El servicio deja el rol en `CLIENTE` y `activo` en `true`, aunque el cuerpo no envíe esos datos.

### Precondiciones

- Personas está arriba en http://localhost:8081 y Swagger abre en http://localhost:8081/doc/swagger-ui.html.
- MySQL está arriba y la base `sakura_personas` existe.
- El correo `ana.soto@ejemplo.cl` no está registrado.
- Tenga a mano un bloc para anotar `idCliente` y `tokenCliente`.

### Pasos

#### 1. Registrar a la clienta

- **Dónde:** Swagger de personas (puerto 8081).
- **Método y ruta:** `POST /api/v1/auth/registro`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "nombre": "Ana",
  "apellido": "Soto",
  "correo": "ana.soto@ejemplo.cl",
  "clave": "AnaClave1",
  "telefono": "+56911112222"
}
```

- **Qué anotar:** el campo `id` de la respuesta. Guárdelo como `idCliente`. Lo usa el caso 2.
- **Resultado esperado:** HTTP 201. El cuerpo es el usuario creado. Debe verse `correo` igual a `ana.soto@ejemplo.cl`, `rol` igual a `CLIENTE` y `activo` en `true`. La clave no vuelve en la respuesta. `nombre` es obligatorio (de 2 a 50 caracteres), `correo` debe ser un email que no exista y `clave` debe tener entre 6 y 100 caracteres. `apellido` (máximo 50) y `telefono` (máximo 20) son opcionales.

#### 2. Iniciar sesión con la clave de registro

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/login`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "ana.soto@ejemplo.cl",
  "clave": "AnaClave1"
}
```

- **Qué anotar:** el campo `token`. Guárdelo como `tokenCliente`. También confirme que `id` coincide con `idCliente`. Este token es el de la primera sesión. El paso 7 entrega otro.
- **Resultado esperado:** HTTP 200. El cuerpo es el usuario e incluye `token`. `correo` y `clave` son obligatorios. Hay un máximo de 5 intentos fallidos en 15 minutos; si se supera, el mismo login responde 401 aunque después escriba la clave correcta, hasta que pasen esos 15 minutos.

#### 3. Verificar la sesión

- **Dónde:** Swagger de personas.
- **Método y ruta:** `GET /api/v1/usuarios/sesion/{token}`
- **Encabezados:** ninguno. El token va en la ruta, no en `Authorization`.
- **Parámetro de ruta:** en `token`, pegue el valor de `tokenCliente` (solo el token, sin la palabra Bearer).
- **Cuerpo:** no lleva cuerpo.
- **Qué anotar:** confirme otra vez `id` (`idCliente`), `correo` y `rol`.
- **Resultado esperado:** HTTP 200 y el mismo usuario del login. Si el token no corresponde a una sesión guardada, la respuesta es 404 con `{"mensaje":"La sesión no es válida"}`.

#### 4. Verificar la clave actual

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/verificar`
- **Encabezados:** `Authorization` con el valor `Bearer ` seguido de `tokenCliente`.
- **Cuerpo:**

```json
{
  "clave": "AnaClave1"
}
```

- **Qué anotar:** nada nuevo. Si este paso falla, no continúe al cambio de clave: la sesión o la clave anotada no coinciden.
- **Resultado esperado:** HTTP 200 y el texto `Clave verificada`.

#### 5. Actualizar el perfil

- **Dónde:** Swagger de personas.
- **Método y ruta:** `PUT /api/v1/usuarios/perfil`
- **Encabezados:** `Authorization` con `Bearer ` y `tokenCliente`.
- **Cuerpo:**

```json
{
  "nombre": "Ana María",
  "apellido": "Soto Rojas",
  "telefono": "+56933334444"
}
```

- **Qué anotar:** nada. `nombre` es obligatorio y debe tener entre 2 y 50 caracteres. `apellido` y `telefono` son opcionales, con los mismos máximos del registro (50 y 20).
- **Resultado esperado:** HTTP 200. El usuario devuelto muestra `nombre` `Ana María`, `apellido` `Soto Rojas` y `telefono` `+56933334444`. El `id`, el `correo` y el `rol` no cambian.

#### 6. Cambiar la clave

- **Dónde:** Swagger de personas.
- **Método y ruta:** `PUT /api/v1/usuarios/clave`
- **Encabezados:** `Authorization` con `Bearer ` y `tokenCliente`.
- **Cuerpo:**

```json
{
  "claveActual": "AnaClave1",
  "claveNueva": "AnaClave2",
  "confirmacion": "AnaClave2"
}
```

- **Qué anotar:** a partir de aquí la clave vigente es `AnaClave2`. `claveNueva` debe tener al menos 6 caracteres. `confirmacion` debe ser exactamente igual a `claveNueva`.
- **Resultado esperado:** HTTP 200 y el texto `La contraseña fue actualizada`. Si `confirmacion` no coincide, HTTP 400 y `{"mensaje":"La confirmación no coincide con la clave nueva"}`. La clave anterior deja de servir para el login.

#### 7. Entrar con la clave nueva

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/login`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "ana.soto@ejemplo.cl",
  "clave": "AnaClave2"
}
```

- **Qué anotar:** reemplace `tokenCliente` por el `token` nuevo de esta respuesta. El token del paso 2 ya no es la sesión guardada, porque cada login escribe un token distinto. El logout del paso siguiente debe usar este token nuevo.
- **Resultado esperado:** HTTP 200, con `id` igual a `idCliente` y un `token` distinto del anterior. Un intento con `AnaClave1` debe responder 401 y `{"mensaje":"Correo o contraseña incorrectos"}`.

#### 8. Cerrar la sesión

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/logout`
- **Encabezados:** `Authorization` con `Bearer ` y el `tokenCliente` del paso 7 (el token nuevo).
- **Cuerpo:** no lleva cuerpo.
- **Qué anotar:** después de este paso ese token queda invalidado. El caso 2 no lo necesita: entra con el administrador de semilla.
- **Resultado esperado:** HTTP 200 y el texto `Sesión cerrada`.

### Checklist de éxito

- El registro respondió 201 con rol `CLIENTE` y `activo` en `true`.
- Anotó `idCliente` y un `tokenCliente`.
- `GET /api/v1/usuarios/sesion/{token}` devolvió a Ana con ese mismo id.
- La verificación de clave respondió `Clave verificada`.
- El perfil quedó con el nombre `Ana María`, el apellido `Soto Rojas` y el teléfono nuevo.
- El cambio de clave respondió `La contraseña fue actualizada`.
- El login con `AnaClave2` respondió 200 y entregó un token nuevo.
- El logout con ese token nuevo respondió `Sesión cerrada`.

---

## Caso 2 — Administración de usuarios en personas

### Objetivo

Comprobar que el administrador de semilla puede crear otro administrador, desactivar a la clienta del caso 1, comprobar que esa cuenta no inicia sesión, reactivarla y ver que vuelve a entrar. Al final hay una variante corta para recuperar y restablecer la clave. El identificador de recuperación no viaja en la respuesta HTTP: se imprime en la consola del proceso de personas y dura 30 minutos.

### Precondiciones

- El caso 1 terminó y tiene anotado `idCliente`.
- La clave vigente de Ana, antes de la variante final, es `AnaClave2`.
- Personas sigue en http://localhost:8081.
- Tiene abierta la consola donde se inició el servicio de personas. Ahí aparecerá el UUID de recuperación.
- El correo `lucia.admin@ejemplo.cl` no está registrado.
- Anote `tokenAdmin` en el bloc. No reutilice el token de Ana.

### Pasos

#### 1. Entrar como administrador de semilla

- **Dónde:** Swagger de personas (puerto 8081).
- **Método y ruta:** `POST /api/v1/auth/login`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "admin@sakura.cl",
  "clave": "Sakura1&"
}
```

- **Qué anotar:** el campo `token`. Guárdelo como `tokenAdmin`. Confirme que `rol` es `ADMIN`. Este mismo token se usa en personal en el caso 3.
- **Resultado esperado:** HTTP 200 y un usuario con `correo` `admin@sakura.cl`, `rol` `ADMIN` y `token` con valor.

#### 2. Crear otra cuenta de administración

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/usuarios`
- **Encabezados:** `Authorization` con `Bearer ` y `tokenAdmin`. Sin ese encabezado, o con el token de Ana, la operación no debe crear el usuario.
- **Cuerpo:** es el mismo conjunto de campos del registro. El servicio ignora un rol enviado por el cliente y asigna `ADMIN`.

```json
{
  "nombre": "Lucía",
  "apellido": "Herrera",
  "correo": "lucia.admin@ejemplo.cl",
  "clave": "LuciaAdmin1",
  "telefono": "+56955556666"
}
```

- **Qué anotar:** el `id` de Lucía, por si quiere inspeccionarlo después. No hace falta iniciar sesión con ella para el resto de la guía. `nombre` (2 a 50), `correo` único y `clave` (6 a 100) son obligatorios. `apellido` y `telefono` son opcionales.
- **Resultado esperado:** HTTP 201. En el cuerpo, `rol` es `ADMIN`, `activo` es `true` y el correo es `lucia.admin@ejemplo.cl`.

#### 3. Desactivar a la clienta del caso 1

- **Dónde:** Swagger de personas.
- **Método y ruta:** `PUT /api/v1/usuarios/{id}/activo`
- **Encabezados:** `Authorization` con `Bearer ` y `tokenAdmin`.
- **Parámetro de ruta:** en `id`, escriba `idCliente` (el id de Ana, no el de Lucía ni el del administrador de semilla).
- **Cuerpo:**

```json
{
  "activo": false
}
```

- **Qué anotar:** deje constancia de que la cuenta quedó inactiva. Un cuerpo vacío `{}` también deja `activo` en `false`. Al desactivar, el servicio anula el token de esa cuenta.
- **Resultado esperado:** HTTP 200 y el usuario de Ana con `activo` en `false`. Solo un administrador puede llamar esta ruta.

#### 4. Comprobar que la clienta desactivada no entra

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/login`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "ana.soto@ejemplo.cl",
  "clave": "AnaClave2"
}
```

- **Qué anotar:** nada. No copie un token de esta respuesta, porque no debe haber sesión.
- **Resultado esperado:** HTTP 401 y `{"mensaje":"Correo o contraseña incorrectos"}`. La cuenta existe, pero está inactiva, así que el login se rechaza con el mismo mensaje que una clave mala. Este intento cuenta dentro del máximo de 5 intentos en 15 minutos. No lo repita muchas veces.

#### 5. Reactivar a la clienta

- **Dónde:** Swagger de personas.
- **Método y ruta:** `PUT /api/v1/usuarios/{id}/activo`
- **Encabezados:** `Authorization` con `Bearer ` y `tokenAdmin`.
- **Parámetro de ruta:** el mismo `idCliente`.
- **Cuerpo:**

```json
{
  "activo": true
}
```

- **Qué anotar:** nada.
- **Resultado esperado:** HTTP 200 y el usuario de Ana con `activo` en `true`.

#### 6. Entrar de nuevo como clienta

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/login`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "ana.soto@ejemplo.cl",
  "clave": "AnaClave2"
}
```

- **Qué anotar:** puede guardar el `token` si quiere seguir probando como Ana. La variante de recuperación no lo usa.
- **Resultado esperado:** HTTP 200, con `activo` en `true` y un `token` nuevo.

#### 7. Variante corta: pedir recuperación de clave

Haga esta variante con Ana ya reactivada. Si la cuenta estuviera inactiva, la respuesta HTTP sería la misma, pero la consola no imprimiría un token.

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/recuperar`
- **Encabezados:** ninguno.
- **Cuerpo:**

```json
{
  "correo": "ana.soto@ejemplo.cl"
}
```

- **Qué anotar:** no busque el token en Swagger. Mire la consola del servidor de personas. Si el correo existe y la cuenta está activa, aparece una línea como esta:

```text
Enlace para restablecer la clave (30 minutos): /api/v1/auth/restablecer?token=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Copie solo el UUID que va después de `token=`. Guárdelo como `tokenRecuperacion`. Caduca a los 30 minutos. La línea anterior de la consola indica `Para: ana.soto@ejemplo.cl`.

- **Resultado esperado:** HTTP 200 y el texto fijo `Si el correo está registrado, enviamos un enlace para restablecer la clave`. Ese texto no cambia cuando el correo no existe. Así la API no revela si la cuenta está registrada.

#### 8. Variante corta: restablecer la clave

- **Dónde:** Swagger de personas.
- **Método y ruta:** `POST /api/v1/auth/restablecer`
- **Encabezados:** ninguno. El identificador va en el cuerpo, no en `Authorization`.
- **Cuerpo:** reemplace el token de ejemplo por el UUID copiado de la consola. La clave nueva debe tener entre 6 y 100 caracteres.

```json
{
  "token": "pegar-aqui-el-uuid-de-la-consola",
  "clave": "AnaClave3"
}
```

- **Qué anotar:** la clave vigente de Ana pasa a ser `AnaClave3`. El token de recuperación se consume: no sirve una segunda vez.
- **Resultado esperado:** HTTP 200 y el texto `La contraseña fue actualizada`.

Para cerrar la variante, puede repetir el login con `AnaClave3`. Debe responder 200. El login con `AnaClave2` debe responder 401.

### Checklist de éxito

- El login de `admin@sakura.cl` respondió 200 con rol `ADMIN`, y anotó `tokenAdmin`.
- `POST /api/v1/usuarios` creó a Lucía con HTTP 201 y rol `ADMIN`.
- Desactivar a Ana (`idCliente`) respondió 200 con `activo` en `false`.
- El login de Ana, mientras estaba inactiva, respondió 401.
- Reactivarla respondió 200 con `activo` en `true`, y el login con `AnaClave2` volvió a responder 200.
- Recuperar clave respondió el texto fijo, y el UUID salió en la consola del servidor, no en Swagger.
- Restablecer respondió `La contraseña fue actualizada`.

---
