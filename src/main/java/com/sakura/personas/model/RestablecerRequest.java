package com.sakura.personas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RestablecerRequest {

	@NotBlank(message = "El token es obligatorio")
	private String token;

	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
	private String clave;

}
