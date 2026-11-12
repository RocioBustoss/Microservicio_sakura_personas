package com.sakura.personas.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
	@Column(nullable = false, length = 50)
	private String nombre;

	@Size(max = 50, message = "El apellido no puede superar 50 caracteres")
	@Column(length = 50)
	private String apellido;

	@Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
	@Column(length = 20)
	private String telefono;

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "El correo no es válido")
	@Column(nullable = false, unique = true, length = 250)
	private String correo;

	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(nullable = false, length = 100)
	private String clave;

	@Column(nullable = false, length = 20)
	private String rol;

	@Column(nullable = false)
	private int intentosFallidos;

	private LocalDateTime ultimoIntento;

	@Column(length = 80)
	private String token;

	@Column(nullable = false)
	private boolean activo = true;

	@JsonIgnore
	@Column(length = 80)
	private String tokenRecuperacion;

	@JsonIgnore
	private LocalDateTime tokenRecuperacionExpira;

}
