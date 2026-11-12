package com.sakura.personas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.personas.config.TokenSoporte;
import com.sakura.personas.model.CorreoRequest;
import com.sakura.personas.model.LoginRequest;
import com.sakura.personas.model.RestablecerRequest;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.model.VerificarRequest;
import com.sakura.personas.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@Autowired
	private UsuarioService usuarioService;

	@PostMapping("/registro")
	public ResponseEntity<Usuario> registro(@Valid @RequestBody Usuario usuario) {
		Usuario creado = usuarioService.registrar(usuario);
		return new ResponseEntity<>(creado, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<Usuario> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(usuarioService.login(request.getCorreo(), request.getClave()));
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		usuarioService.logout(TokenSoporte.extraer(authorization));
		return ResponseEntity.ok("Sesión cerrada");
	}

	@PostMapping("/recuperar")
	public ResponseEntity<String> recuperar(@Valid @RequestBody CorreoRequest request) {
		return ResponseEntity.ok(usuarioService.recuperar(request.getCorreo()));
	}

	@PostMapping("/restablecer")
	public ResponseEntity<String> restablecer(@Valid @RequestBody RestablecerRequest request) {
		usuarioService.restablecer(request.getToken(), request.getClave());
		return ResponseEntity.ok("La contraseña fue actualizada");
	}

	@PostMapping("/verificar")
	public ResponseEntity<String> verificar(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody VerificarRequest request) {
		usuarioService.verificarClave(TokenSoporte.extraer(authorization), request.getClave());
		return ResponseEntity.ok("Clave verificada");
	}

}
