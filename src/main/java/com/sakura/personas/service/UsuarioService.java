package com.sakura.personas.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sakura.personas.exception.AccesoDenegadoException;
import com.sakura.personas.exception.CredencialesInvalidasException;
import com.sakura.personas.exception.NoAutenticadoException;
import com.sakura.personas.exception.RecursoNoEncontradoException;
import com.sakura.personas.exception.ReglaNegocioException;
import com.sakura.personas.model.CambioClaveRequest;
import com.sakura.personas.model.PerfilRequest;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.repository.UsuarioRepository;

@Service
public class UsuarioService {

	public static final int MAX_INTENTOS = 5;
	public static final int MINUTOS_BLOQUEO = 15;
	public static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos";
	public static final String MENSAJE_RECUPERAR = "Si el correo está registrado, enviamos un enlace para restablecer la clave";
	public static final int MINUTOS_RECUPERACION = 30;

	@Autowired
	private UsuarioRepository usuarioRepository;

	public Usuario registrar(Usuario datos) {
		if (usuarioRepository.existsByCorreo(datos.getCorreo())) {
			throw new ReglaNegocioException("El correo ya está registrado");
		}
		datos.setId(null);
		datos.setRol("CLIENTE");
		datos.setActivo(true);
		datos.setIntentosFallidos(0);
		datos.setUltimoIntento(null);
		datos.setToken(null);
		return usuarioRepository.save(datos);
	}

	public Usuario login(String correo, String clave) {
		Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
		if (usuario == null || !usuario.isActivo()) {
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}
		if (estaBloqueado(usuario)) {
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}
		if (!usuario.getClave().equals(clave)) {
			usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
			usuario.setUltimoIntento(LocalDateTime.now());
			usuarioRepository.save(usuario);
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}
		usuario.setIntentosFallidos(0);
		usuario.setUltimoIntento(null);
		usuario.setToken(UUID.randomUUID().toString());
		return usuarioRepository.save(usuario);
	}

	public void logout(String token) {
		Usuario usuario = usuarioRepository.findByToken(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("No hay una sesión activa con ese token"));
		usuario.setToken(null);
		usuarioRepository.save(usuario);
	}

	public Usuario buscarPorToken(String token) {
		return usuarioRepository.findByToken(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("La sesión no es válida"));
	}

	public String recuperar(String correo) {
		Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
		if (usuario != null && usuario.isActivo()) {
			String token = UUID.randomUUID().toString();
			usuario.setTokenRecuperacion(token);
			usuario.setTokenRecuperacionExpira(LocalDateTime.now().plusMinutes(MINUTOS_RECUPERACION));
			usuarioRepository.save(usuario);
			System.out.println("Para: " + usuario.getCorreo());
			System.out.println("Enlace para restablecer la clave (30 minutos): /api/v1/auth/restablecer?token=" + token);
		}
		return MENSAJE_RECUPERAR;
	}

	public void restablecer(String token, String clave) {
		Usuario usuario = usuarioRepository.findByTokenRecuperacion(token)
				.orElseThrow(() -> new ReglaNegocioException("El enlace de recuperación no es válido"));
		if (usuario.getTokenRecuperacionExpira() == null
				|| usuario.getTokenRecuperacionExpira().isBefore(LocalDateTime.now())) {
			throw new ReglaNegocioException("El enlace de recuperación venció");
		}
		usuario.setClave(clave);
		usuario.setTokenRecuperacion(null);
		usuario.setTokenRecuperacionExpira(null);
		usuario.setIntentosFallidos(0);
		usuario.setUltimoIntento(null);
		usuarioRepository.save(usuario);
	}

	public Usuario crearAdmin(String tokenAdmin, Usuario datos) {
		exigirAdmin(tokenAdmin);
		if (usuarioRepository.existsByCorreo(datos.getCorreo())) {
			throw new ReglaNegocioException("El correo ya está registrado");
		}
		datos.setId(null);
		datos.setRol("ADMIN");
		datos.setActivo(true);
		datos.setIntentosFallidos(0);
		datos.setUltimoIntento(null);
		datos.setToken(null);
		datos.setTokenRecuperacion(null);
		return usuarioRepository.save(datos);
	}

	public Usuario cambiarActivo(String tokenAdmin, Long id, boolean activo) {
		exigirAdmin(tokenAdmin);
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario"));
		usuario.setActivo(activo);
		if (!activo) {
			usuario.setToken(null);
		}
		return usuarioRepository.save(usuario);
	}

	public Usuario actualizarPerfil(String token, PerfilRequest datos) {
		Usuario usuario = buscarPorToken(token);
		usuario.setNombre(datos.getNombre());
		usuario.setApellido(datos.getApellido());
		usuario.setTelefono(datos.getTelefono());
		return usuarioRepository.save(usuario);
	}

	public void cambiarClave(String token, CambioClaveRequest datos) {
		Usuario usuario = buscarPorToken(token);
		if (!usuario.getClave().equals(datos.getClaveActual())) {
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}
		usuario.setClave(datos.getClaveNueva());
		usuarioRepository.save(usuario);
	}

	public void verificarClave(String token, String clave) {
		Usuario usuario = buscarPorToken(token);
		if (!usuario.isActivo() || !usuario.getClave().equals(clave)) {
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}
	}

	private void exigirAdmin(String token) {
		if (token == null || token.isBlank()) {
			throw new NoAutenticadoException("Debe iniciar sesión");
		}
		Usuario usuario = usuarioRepository.findByToken(token)
				.orElseThrow(() -> new NoAutenticadoException("Debe iniciar sesión"));
		if (!usuario.isActivo() || !"ADMIN".equals(usuario.getRol())) {
			throw new AccesoDenegadoException("No tiene permiso para esta acción");
		}
	}

	private boolean estaBloqueado(Usuario usuario) {
		if (usuario.getIntentosFallidos() < MAX_INTENTOS || usuario.getUltimoIntento() == null) {
			return false;
		}
		return usuario.getUltimoIntento().plusMinutes(MINUTOS_BLOQUEO).isAfter(LocalDateTime.now());
	}

}
