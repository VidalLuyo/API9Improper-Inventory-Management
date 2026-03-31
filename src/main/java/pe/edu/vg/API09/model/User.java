package pe.edu.vg.API09.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modelo completo de usuario (incluye datos sensibles)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String token;
}
