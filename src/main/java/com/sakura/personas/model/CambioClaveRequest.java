package com.sakura.personas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambioClaveRequest {

	@NotBlank(message = "La contraseña actual es obligatoria")
	private String claveActual;

	@NotBlank(message = "La contraseña nueva es obligatoria")
	@Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
	private String claveNueva;

	@NotBlank(message = "Debe confirmar la contraseña")
	private String confirmacion;

}
