package com.sakura.personas.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CorreoRequest {

	@NotBlank(message = "El correo es obligatorio")
	private String correo;

}
