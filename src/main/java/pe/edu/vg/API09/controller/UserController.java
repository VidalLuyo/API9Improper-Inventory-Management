package pe.edu.vg.API09.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.vg.API09.model.User;
import pe.edu.vg.API09.model.UserDTO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Controlador con dos versiones de la API
// v1 es la versión antigua sin seguridad
// v2 es la versión nueva con seguridad
@RestController
public class UserController {

    // Lista de usuarios de prueba
    private final List<User> users = Arrays.asList(
        new User(1L, "Juan Pérez", "juan@empresa.com", "password123", "token_abc123xyz"),
        new User(2L, "María García", "maria@empresa.com", "qwerty456", "token_def456uvw"),
        new User(3L, "Carlos López", "carlos@empresa.com", "admin2024", "token_ghi789rst"),
        new User(4L, "Ana Martínez", "ana@empresa.com", "secret789", "token_jkl012mno")
    );

    // API v1 - Sin autenticación, expone todos los datos
    @GetMapping("/api/v1/users")
    public ResponseEntity<List<User>> getUsersV1() {
        System.out.println("⚠️ Acceso a v1 sin autenticación");
        return ResponseEntity.ok(users);
    }

    // Mensaje de advertencia sobre v1
    @GetMapping("/api/v1/users/deprecated")
    public ResponseEntity<String> getDeprecatedWarning() {
        return ResponseEntity.ok(
            "⚠️ Esta API está deprecada. Use /api/v2/users con autenticación."
        );
    }

    // API v2 - Con autenticación, solo expone datos seguros
    @GetMapping("/api/v2/users")
    public ResponseEntity<List<UserDTO>> getUsersV2() {
        System.out.println("✅ Acceso a v2 con autenticación");
        
        List<UserDTO> userDTOs = users.stream()
            .map(user -> new UserDTO(user.getId(), user.getNombre(), user.getEmail()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(userDTOs);
    }
}
