package com.sakura.personas.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarRequest {

	@NotBlank(message = "La contraseña es obligatoria")
	private String clave;

}
