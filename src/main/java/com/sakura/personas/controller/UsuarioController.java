package com.sakura.personas.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.personas.config.TokenSoporte;
import com.sakura.personas.model.ActivoRequest;
import com.sakura.personas.model.CambioClaveRequest;
import com.sakura.personas.model.PerfilRequest;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

	@Autowired
	private UsuarioService usuarioService;

	@GetMapping("/sesion/{token}")
	public ResponseEntity<Usuario> sesion(@PathVariable String token) {
		return ResponseEntity.ok(usuarioService.buscarPorToken(token));
	}

	@PostMapping
	public ResponseEntity<Usuario> crearAdmin(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody Usuario usuario) {
		Usuario creado = usuarioService.crearAdmin(TokenSoporte.extraer(authorization), usuario);
		return new ResponseEntity<>(creado, HttpStatus.CREATED);
	}

	@PutMapping("/perfil")
	public Usuario perfil(@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody PerfilRequest datos) {
		return usuarioService.actualizarPerfil(TokenSoporte.extraer(authorization), datos);
	}

	@PutMapping("/clave")
	public ResponseEntity<?> clave(@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody CambioClaveRequest datos) {
		if (!datos.getClaveNueva().equals(datos.getConfirmacion())) {
			return ResponseEntity.badRequest().body(Map.of("mensaje", "La confirmación no coincide con la clave nueva"));
		}
		usuarioService.cambiarClave(TokenSoporte.extraer(authorization), datos);
		return ResponseEntity.ok("La contraseña fue actualizada");
	}

	@PutMapping("/{id}/activo")
	public Usuario activo(@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id, @RequestBody ActivoRequest datos) {
		return usuarioService.cambiarActivo(TokenSoporte.extraer(authorization), id, datos.isActivo());
	}

}
