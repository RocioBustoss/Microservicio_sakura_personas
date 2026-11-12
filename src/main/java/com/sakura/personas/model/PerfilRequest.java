package com.sakura.personas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilRequest {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
	private String nombre;

	@Size(max = 50, message = "El apellido no puede superar 50 caracteres")
	private String apellido;

	@Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
	private String telefono;

}
