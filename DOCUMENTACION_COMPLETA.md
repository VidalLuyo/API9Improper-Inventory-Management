# 🔴 DEMO: OWASP API9:2023 - Improper Inventory Management

## 📋 ¿Qué es este proyecto?

Imagina que tienes una empresa y creas una nueva versión de tu sistema (v2) con mejor seguridad, pero te olvidas de apagar la versión antigua (v1). 

Este proyecto simula exactamente eso:
- **API v1** = Versión antigua SIN seguridad (cualquiera puede entrar)
- **API v2** = Versión nueva CON seguridad (necesitas contraseña para entrar)

El problema es que la v1 sigue funcionando y un atacante puede usarla para robar información.

---

## 🎯 ¿Qué vamos a demostrar?

En 5 minutos vas a ver cómo:
1. Un atacante puede entrar a la API v1 sin contraseña
2. La API v1 le muestra passwords y tokens (información sensible)
3. La API v2 sí pide contraseña y solo muestra información segura

Es como tener una puerta trasera abierta en tu casa mientras la puerta principal tiene cerradura.

---

## 📁 Estructura del Proyecto Creado

```
API09/
├── src/main/java/pe/edu/vg/API09/
│   ├── Api09Application.java              # Aplicación principal Spring Boot
│   │
│   ├── config/
│   │   ├── SecurityConfig.java            # Configuración de seguridad
│   │   │                                  # - /api/v1/** → permitAll() (VULNERABLE)
│   │   │                                  # - /api/v2/** → authenticated() (SEGURO)
│   │   │
│   │   └── SimpleTokenAuthFilter.java     # Filtro de autenticación por token
│   │                                      # - Valida "Bearer 12345"
│   │                                      # - Solo aplica a rutas protegidas
│   │
│   ├── controller/
│   │   └── UserController.java            # Controlador con endpoints
│   │                                      # - GET /api/v1/users (vulnerable)
│   │                                      # - GET /api/v1/users/deprecated
│   │                                      # - GET /api/v2/users (seguro)
│   │
│   └── model/
│       ├── User.java                      # Modelo completo con datos sensibles
│       │                                  # - id, nombre, email, password, token
│       │
│       └── UserDTO.java                   # DTO filtrado sin datos sensibles
│                                          # - id, nombre, email (sin password/token)
│
├── src/main/resources/
│   └── application.yaml                   # Configuración de la aplicación
│                                          # - Puerto 8080
│                                          # - Logging configurado
│
└── pom.xml                                # Dependencias Maven
                                           # - Spring Web
                                           # - Spring Security
                                           # - Lombok
```

---

## 🏗️ ARQUITECTURA DEL DEMO

### 📊 Flujo de la Vulnerabilidad

```
┌─────────────────────────────────────────────────────────────┐
│                    ATACANTE / USUARIO                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │                                         │
        │   🔴 /api/v1/users    🟢 /api/v2/users │
        │   (SIN seguridad)     (CON seguridad)  │
        │                                         │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │       SecurityConfig.java               │
        │  ┌───────────────────────────────────┐  │
        │  │ /api/v1/** → permitAll()          │  │
        │  │ /api/v2/** → authenticated()      │  │
        │  └───────────────────────────────────┘  │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │     SimpleTokenAuthFilter.java          │
        │  ┌───────────────────────────────────┐  │
        │  │ Valida: "Bearer 12345"            │  │
        │  │ Solo para /api/v2/**              │  │
        │  └───────────────────────────────────┘  │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │       UserController.java               │
        │  ┌───────────────────────────────────┐  │
        │  │ getUsersV1() → User (completo)    │  │
        │  │ getUsersV2() → UserDTO (filtrado) │  │
        │  └───────────────────────────────────┘  │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │           RESPUESTA                     │
        │                                         │
        │  🔴 v1: {id, nombre, email,            │
        │         password, token}                │
        │                                         │
        │  🟢 v2: {id, nombre, email}            │
        └─────────────────────────────────────────┘
```

---

## 🔍 COMPONENTES IMPLEMENTADOS

### 1. SecurityConfig.java
**Propósito:** Configurar qué rutas requieren autenticación

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").permitAll()      // ❌ VULNERABLE
                .requestMatchers("/api/v2/**").authenticated()  // ✅ SEGURO
            )
            .addFilterBefore(new SimpleTokenAuthFilter(), ...);
    }
}
```

**Características:**
- `/api/v1/**` → Acceso libre sin autenticación (VULNERABLE)
- `/api/v2/**` → Requiere autenticación (SEGURO)
- CSRF deshabilitado para simplificar el demo

---

### 2. SimpleTokenAuthFilter.java
**Propósito:** Validar el token "Bearer 12345"

```java
public class SimpleTokenAuthFilter extends OncePerRequestFilter {
    private static final String VALID_TOKEN = "12345";
    
    @Override
    protected void doFilterInternal(...) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (VALID_TOKEN.equals(token)) {
                // Autenticar usuario
            }
        }
    }
}
```

**Características:**
- Solo se aplica a rutas protegidas (/api/v2/**)
- Token válido: "12345"
- En producción: usar JWT real, OAuth2, etc.

---

### 3. UserController.java
**Propósito:** Exponer los endpoints v1 y v2

```java
@RestController
public class UserController {
    
    // 🔴 VULNERABLE - Sin autenticación, expone datos sensibles
    @GetMapping("/api/v1/users")
    public ResponseEntity<List<User>> getUsersV1() {
        return ResponseEntity.ok(users); // Incluye password y token
    }
    
    // 🟢 SEGURO - Con autenticación, datos filtrados
    @GetMapping("/api/v2/users")
    public ResponseEntity<List<UserDTO>> getUsersV2() {
        List<UserDTO> userDTOs = users.stream()
            .map(user -> new UserDTO(user.getId(), user.getNombre(), user.getEmail()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs); // Sin password ni token
    }
    
    // ⚠️ ADVERTENCIA
    @GetMapping("/api/v1/users/deprecated")
    public ResponseEntity<String> getDeprecatedWarning() {
        return ResponseEntity.ok("Esta API está deprecada...");
    }
}
```

**Características:**
- `getUsersV1()` → Retorna `User` completo (con password y token)
- `getUsersV2()` → Retorna `UserDTO` filtrado (sin datos sensibles)
- Incluye logs informativos en consola

---

### 4. Modelos de Datos

**User.java** (Completo - con datos sensibles)
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String nombre;
    private String email;
    private String password;  // ⚠️ Sensible
    private String token;     // ⚠️ Sensible
}
```

**UserDTO.java** (Filtrado - sin datos sensibles)
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String nombre;
    private String email;
    // ✅ NO incluye password ni token
}
```

---

## 🎯 ENDPOINTS IMPLEMENTADOS

| Endpoint | Método | Autenticación | Datos Expuestos | Estado |
|----------|--------|---------------|-----------------|--------|
| `/api/v1/users` | GET | ❌ NO | id, nombre, email, **password**, **token** | 🔴 VULNERABLE |
| `/api/v1/users/deprecated` | GET | ❌ NO | Mensaje de advertencia | ⚠️ Deprecado |
| `/api/v2/users` | GET | ✅ SÍ (Bearer 12345) | id, nombre, email | 🟢 SEGURO |

---

## 📊 DATOS DE PRUEBA

El sistema incluye 4 usuarios hardcodeados en memoria:

| ID | Nombre | Email | Password (v1) | Token (v1) |
|----|--------|-------|---------------|------------|
| 1 | Juan Pérez | juan@empresa.com | password123 | token_abc123xyz |
| 2 | María García | maria@empresa.com | qwerty456 | token_def456uvw |
| 3 | Carlos López | carlos@empresa.com | admin2024 | token_ghi789rst |
| 4 | Ana Martínez | ana@empresa.com | secret789 | token_jkl012mno |

---

## 🚀 Cómo Ejecutar el Proyecto

### Paso 1: Abrir terminal en la carpeta del proyecto

### Paso 2: Ejecutar el comando según tu sistema

**Windows (PowerShell o CMD):**
```bash
mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run
```

### Paso 3: Esperar a que inicie

Verás en la consola: `Started Api09Application`

### Paso 4: La aplicación está lista

**URL:** http://localhost:8080

Ahora puedes probar los endpoints con Postman o curl.

---

## 🧪 DEMOSTRACIÓN PASO A PASO

---

## 🟣 OPCIÓN 1: USAR POSTMAN (Recomendado - Más Fácil)

### 🔴 PRUEBA 1: API v1 - VULNERABLE (sin seguridad)

1. Abre Postman
2. Crea una nueva petición (New Request)
3. Configura:
   - **Método:** GET
   - **URL:** `http://localhost:8080/api/v1/users`
4. Click en **Send**

**✅ Resultado:**
```json
[
  {
    "id": 1,
    "nombre": "Juan Pérez",
    "email": "juan@empresa.com",
    "password": "password123",        ← ⚠️ EXPONE PASSWORD
    "token": "token_abc123xyz"        ← ⚠️ EXPONE TOKEN
  },
  {
    "id": 2,
    "nombre": "María García",
    "email": "maria@empresa.com",
    "password": "qwerty456",
    "token": "token_def456uvw"
  },
  ...
]
```

**🚨 PROBLEMA:** 
- NO pedimos ninguna contraseña o token
- La API nos dio passwords y tokens de usuarios
- Cualquier atacante puede hacer esto

---

### 🟢 PRUEBA 2: API v2 - SEGURA (sin token, debe fallar)

1. En Postman, crea otra petición
2. Configura:
   - **Método:** GET
   - **URL:** `http://localhost:8080/api/v2/users`
3. Click en **Send**

**✅ Resultado:**
```
Status: 401 Unauthorized
```

**👍 CORRECTO:** La API v2 nos rechaza porque no enviamos token de autenticación.

---

### 🟢 PRUEBA 3: API v2 - SEGURA (con token válido)

1. En la misma petición de Postman
2. Ve a la pestaña **Headers**
3. Agrega un nuevo header:
   - **Key:** `Authorization`
   - **Value:** `Bearer 12345`
4. Click en **Send**

**Así se ve en Postman:**
```
Headers:
┌─────────────────┬──────────────┐
│ Key             │ Value        │
├─────────────────┼──────────────┤
│ Authorization   │ Bearer 12345 │
└─────────────────┴──────────────┘
```

**✅ Resultado:**
```json
[
  {
    "id": 1,
    "nombre": "Juan Pérez",
    "email": "juan@empresa.com"
    // ✅ NO incluye password ni token
  },
  {
    "id": 2,
    "nombre": "María García",
    "email": "maria@empresa.com"
  },
  ...
]
```

**👍 CORRECTO:**
- Ahora sí nos dejó entrar (porque enviamos el token)
- Pero solo nos muestra información segura (sin passwords ni tokens)

---

### 🔴 PRUEBA 4: API v2 con token INCORRECTO

1. En Postman, cambia el header:
   - **Key:** `Authorization`
   - **Value:** `Bearer token_falso`
2. Click en **Send**

**✅ Resultado:**
```
Status: 401 Unauthorized
```

**👍 CORRECTO:** La API detecta que el token es falso y nos rechaza.

---

### ⚠️ PRUEBA 5: Mensaje de deprecación

1. En Postman, crea nueva petición
2. Configura:
   - **Método:** GET
   - **URL:** `http://localhost:8080/api/v1/users/deprecated`
3. Click en **Send**

**✅ Resultado:**
```
⚠️ ADVERTENCIA: Esta API (v1) está deprecada y debería estar desactivada.
Por favor use /api/v2/users con autenticación apropiada.
```

---

## 💻 OPCIÓN 2: USAR CURL (Línea de comandos)

Si prefieres usar la terminal en lugar de Postman:

### 🔴 PRUEBA 1: API v1 vulnerable
```bash
curl http://localhost:8080/api/v1/users
```

### 🟢 PRUEBA 2: API v2 sin token (falla)
```bash
curl http://localhost:8080/api/v2/users
```

### 🟢 PRUEBA 3: API v2 con token válido
```bash
curl -H "Authorization: Bearer 12345" http://localhost:8080/api/v2/users
```

### 🔴 PRUEBA 4: API v2 con token inválido
```bash
curl -H "Authorization: Bearer token_falso" http://localhost:8080/api/v2/users
```

### ⚠️ PRUEBA 5: Mensaje de deprecación
```bash
curl http://localhost:8080/api/v1/users/deprecated
```

---

## 🪟 OPCIÓN 3: USAR POWERSHELL (Windows)

Si usas PowerShell en Windows:

### 🔴 PRUEBA 1: API v1 vulnerable
```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/v1/users | Select-Object -Expand Content
```

### 🟢 PRUEBA 2: API v2 sin token (falla)
```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/v2/users
```

### 🟢 PRUEBA 3: API v2 con token válido
```powershell
$headers = @{"Authorization"="Bearer 12345"}
Invoke-WebRequest -Uri http://localhost:8080/api/v2/users -Headers $headers | Select-Object -Expand Content
```

---

## 📊 RESUMEN DE LAS PRUEBAS

| Prueba | Endpoint | Token | Resultado | ¿Qué demuestra? |
|--------|----------|-------|-----------|-----------------|
| 1 | `/api/v1/users` | ❌ NO | ✅ Funciona y expone passwords | 🚨 VULNERABLE |
| 2 | `/api/v2/users` | ❌ NO | ❌ Error 401 | ✅ Protegida |
| 3 | `/api/v2/users` | ✅ SÍ (12345) | ✅ Funciona sin passwords | ✅ Segura |
| 4 | `/api/v2/users` | ⚠️ Token falso | ❌ Error 401 | ✅ Valida tokens |
| 5 | `/api/v1/users/deprecated` | ❌ NO | ⚠️ Advertencia | ℹ️ Informativo |

---

## 🎯 ESCENARIO DE ATAQUE

### Paso 1: Descubrimiento
```
Atacante encuentra documentación antigua o hace fuzzing de endpoints:
→ Descubre /api/v1/users
```

### Paso 2: Explotación
```bash
curl http://localhost:8080/api/v1/users
→ Obtiene passwords y tokens SIN autenticación
```

### Paso 3: Escalación
```
Usa los tokens robados para:
- Acceder a otras APIs
- Suplantar identidades
- Acceso no autorizado a recursos
- Robo de información sensible
```

---

## 🔍 EXPLICACIÓN SIMPLE DE LA VULNERABILIDAD

### ❌ ¿Cuál es el problema?

Imagina esta situación en la vida real:

1. **Tienes una tienda con puerta vieja (v1)** que no tiene cerradura
2. **Instalas una puerta nueva (v2)** con cerradura moderna
3. **Pero te olvidas de cerrar la puerta vieja** ← ESTE ES EL PROBLEMA
4. **Un ladrón encuentra la puerta vieja** y entra sin problemas

Eso es exactamente lo que pasa aquí:
- **API v1** = Puerta vieja sin cerradura (sin seguridad)
- **API v2** = Puerta nueva con cerradura (con seguridad)
- **Problema** = La v1 sigue abierta y funcionando

### 🎯 ¿Por qué es peligroso?

**Para la empresa:**
- No saben que la API v1 sigue activa
- Nadie la está vigilando
- Expone información sensible (passwords, tokens)

**Para el atacante:**
- Puede encontrar la v1 buscando en Google, documentación antigua, o probando URLs
- Entra sin contraseña
- Roba passwords y tokens
- Usa esos datos para hacer más daño

### 🌍 Ejemplo del mundo real:

```
Atacante busca en Google: "api.empresa.com v1"
→ Encuentra documentación antigua
→ Prueba: http://api.empresa.com/api/v1/users
→ ¡Funciona! Y le da passwords de usuarios
→ Usa esos passwords para entrar al sistema
```

### 📝 Esto se llama: "Improper Inventory Management"

**Traducción:** "No saber qué APIs tienes activas"

Es como tener 10 puertas en tu casa pero solo recordar 5. Las otras 5 están abiertas y no lo sabes.

---

## ✅ SOLUCIONES IMPLEMENTADAS Y PROPUESTAS

### Solución 1: Desactivar v1 completamente
```java
@GetMapping("/api/v1/users")
public ResponseEntity<?> getUsersV1() {
    return ResponseEntity
        .status(HttpStatus.GONE)
        .body("Esta API ha sido desactivada permanentemente");
}
```

### Solución 2: Aplicar autenticación a v1
```java
.requestMatchers("/api/v1/**").authenticated()
```

### Solución 3: Filtrar datos sensibles en v1
```java
@GetMapping("/api/v1/users")
public ResponseEntity<List<UserDTO>> getUsersV1() {
    // Retornar UserDTO en lugar de User
    return ResponseEntity.ok(userDTOs);
}
```

### Solución 4: Monitoreo y alertas
- Registrar todos los accesos a v1
- Alertar cuando se use v1
- Analizar logs para detectar ataques
- Dashboard de APIs activas

### Solución 5: Inventario de APIs
- Mantener lista de todas las versiones activas
- Proceso de deprecación formal
- Revisión periódica de endpoints expuestos
- Documentación actualizada

---

## 📈 COMPARACIÓN DE SEGURIDAD

| Aspecto | API v1 (Vulnerable) | API v2 (Segura) |
|---------|---------------------|-----------------|
| Autenticación requerida | ❌ NO | ✅ SÍ |
| Expone password | ❌ SÍ | ✅ NO |
| Expone token | ❌ SÍ | ✅ NO |
| Filtrado de datos | ❌ NO | ✅ SÍ |
| Estado | ⚠️ Debería estar desactivada | ✅ Activa y protegida |
| Logging | ⚠️ Básico | ⚠️ Básico |
| Rate limiting | ❌ NO | ❌ NO (no implementado en demo) |
| Documentación | ❌ Desactualizada | ✅ Actualizada |

---

## 🛠️ DEPENDENCIAS UTILIZADAS

```xml
<!-- Spring Boot 4.0.5 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 🎓 CONCEPTOS DEMOSTRADOS

### 1. Improper Inventory Management
- API antigua sin desactivar
- Falta de control de versiones
- Exposición de datos sensibles
- Sin inventario de endpoints activos

### 2. Autenticación y Autorización
- Token Bearer simple
- Filtro de seguridad personalizado
- Configuración de Spring Security
- Diferencia entre autenticado y no autenticado

### 3. Filtrado de Datos
- DTO vs Modelo completo
- Separación de datos públicos y sensibles
- Principio de mínimo privilegio

### 4. Buenas Prácticas
- Versionado de APIs
- Deprecación controlada
- Logging de accesos
- Mensajes informativos

---

## 💡 PUNTOS CLAVE PARA LA PRESENTACIÓN

1. **v1 nunca fue desactivada** → Improper Inventory Management
2. **Atacante puede descubrir v1** mediante fuzzing o documentación antigua
3. **Datos sensibles expuestos** sin autenticación
4. **v2 implementa seguridad correcta** pero v1 sigue siendo un vector de ataque
5. **Solución:** Desactivar v1 completamente y mantener inventario de APIs

---

## 🛠️ Troubleshooting

### Puerto 8080 ocupado
```yaml
# Cambiar en application.yaml
server:
  port: 8081
```

### Maven no funciona
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac - dar permisos
chmod +x mvnw
./mvnw spring-boot:run
```

### Ver logs detallados
Observa la consola donde ejecutaste la aplicación, verás:
- `⚠️ [VULNERABLE] Acceso a /api/v1/users SIN autenticación`
- `✅ [SEGURO] Acceso a /api/v2/users CON autenticación`
- `✅ Token válido - Acceso autorizado`
- `❌ Token inválido`

---

## 📝 COMANDOS PARA WINDOWS POWERSHELL

Si usas PowerShell en lugar de bash:

```powershell
# API v1 vulnerable
Invoke-WebRequest -Uri http://localhost:8080/api/v1/users | Select-Object -Expand Content

# API v2 sin token (falla)
Invoke-WebRequest -Uri http://localhost:8080/api/v2/users

# API v2 con token (funciona)
Invoke-WebRequest -Uri http://localhost:8080/api/v2/users -Headers @{"Authorization"="Bearer 12345"} | Select-Object -Expand Content
```

---

## 🎬 RESUMEN EJECUTIVO

### ✅ Lo que se implementó:

1. **Proyecto Spring Boot funcional** con Maven
2. **Endpoint v1 vulnerable** sin autenticación que expone datos sensibles
3. **Endpoint v2 seguro** con autenticación y datos filtrados
4. **Configuración de Spring Security** diferenciada por versión
5. **Filtro de autenticación simple** con token Bearer
6. **Modelos separados** (User completo vs UserDTO filtrado)
7. **Datos de prueba** en memoria (4 usuarios)
8. **Logs informativos** en consola
9. **Mensaje de deprecación** para v1

### 🎯 Objetivo cumplido:

Demo funcional que demuestra en **5 minutos** la vulnerabilidad OWASP API9:2023 - Improper Inventory Management, mostrando cómo una API antigua sin desactivar puede exponer datos sensibles.

---

## 📚 RECURSOS ADICIONALES

- [OWASP API Security Top 10 2023](https://owasp.org/API-Security/editions/2023/en/0xa9-improper-inventory-management/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

## ✅ CHECKLIST DE VERIFICACIÓN

- [x] Proyecto Spring Boot funcional
- [x] Endpoint v1 sin autenticación (vulnerable)
- [x] Endpoint v2 con autenticación (seguro)
- [x] Filtrado de datos sensibles en v2
- [x] Configuración de Spring Security
- [x] Filtro de autenticación personalizado
- [x] Datos de prueba en memoria
- [x] Logs informativos en consola
- [x] Mensaje de deprecación
- [x] Documentación completa

---

**🎬 LISTO PARA PRESENTAR**

El proyecto está completo y listo para demostrar la vulnerabilidad OWASP API9:2023 en máximo 5 minutos.
