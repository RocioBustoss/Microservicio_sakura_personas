package com.sakura.personas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakura.personas.exception.AccesoDenegadoException;
import com.sakura.personas.exception.CredencialesInvalidasException;
import com.sakura.personas.exception.NoAutenticadoException;
import com.sakura.personas.exception.RecursoNoEncontradoException;
import com.sakura.personas.exception.ReglaNegocioException;
import com.sakura.personas.model.CambioClaveRequest;
import com.sakura.personas.model.PerfilRequest;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@InjectMocks
	private UsuarioService usuarioService;

	private Usuario usuarioEjemplo;

	@BeforeEach
	void setUp() {
		usuarioEjemplo = new Usuario();
		usuarioEjemplo.setId(1L);
		usuarioEjemplo.setNombre("Sakura");
		usuarioEjemplo.setApellido("Kinomoto");
		usuarioEjemplo.setCorreo("sakura@test.com");
		usuarioEjemplo.setClave("password123");
		usuarioEjemplo.setRol("CLIENTE");
		usuarioEjemplo.setActivo(true);
		usuarioEjemplo.setIntentosFallidos(0);
		usuarioEjemplo.setUltimoIntento(null);
		usuarioEjemplo.setToken(null);
	}

	// -------------------------------------------------------------------------
	// REGISTRAR
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para registrar")
	class RegistrarTests {

		@Test
		@DisplayName("Debe registrar usuario exitosamente como CLIENTE")
		void registrar_Exito() {
			when(usuarioRepository.existsByCorreo("sakura@test.com")).thenReturn(false);
			when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

			Usuario resultado = usuarioService.registrar(usuarioEjemplo);

			assertThat(resultado).isNotNull();
			assertThat(resultado.getRol()).isEqualTo("CLIENTE");
			assertThat(resultado.isActivo()).isTrue();
			assertThat(resultado.getIntentosFallidos()).isZero();
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe lanzar ReglaNegocioException si el correo ya existe")
		void registrar_CorreoDuplicado_LanzaExcepcion() {
			when(usuarioRepository.existsByCorreo("sakura@test.com")).thenReturn(true);

			assertThatThrownBy(() -> usuarioService.registrar(usuarioEjemplo))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El correo ya está registrado");

			verify(usuarioRepository, never()).save(any());
		}
	}

	// -------------------------------------------------------------------------
	// LOGIN
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para login")
	class LoginTests {

		@Test
		@DisplayName("Debe iniciar sesión exitosamente y generar token")
		void login_Exito() {
			when(usuarioRepository.findByCorreo("sakura@test.com")).thenReturn(Optional.of(usuarioEjemplo));
			when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

			Usuario resultado = usuarioService.login("sakura@test.com", "password123");

			assertThat(resultado.getToken()).isNotNull().isNotBlank();
			assertThat(resultado.getIntentosFallidos()).isZero();
			assertThat(resultado.getUltimoIntento()).isNull();
		}

		@Test
		@DisplayName("Debe lanzar CredencialesInvalidasException si el usuario no existe")
		void login_UsuarioNoEncontrado_LanzaExcepcion() {
			when(usuarioRepository.findByCorreo("desconocido@test.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> usuarioService.login("desconocido@test.com", "123456"))
					.isInstanceOf(CredencialesInvalidasException.class)
					.hasMessage(UsuarioService.MENSAJE_CREDENCIALES);
		}

		@Test
		@DisplayName("Debe lanzar CredencialesInvalidasException si el usuario está inactivo")
		void login_UsuarioInactivo_LanzaExcepcion() {
			usuarioEjemplo.setActivo(false);
			when(usuarioRepository.findByCorreo("sakura@test.com")).thenReturn(Optional.of(usuarioEjemplo));

			assertThatThrownBy(() -> usuarioService.login("sakura@test.com", "password123"))
					.isInstanceOf(CredencialesInvalidasException.class)
					.hasMessage(UsuarioService.MENSAJE_CREDENCIALES);
		}

		@Test
		@DisplayName("Debe incrementar intentos fallidos si la clave es incorrecta")
		void login_ClaveIncorrecta_IncrementaIntentos() {
			when(usuarioRepository.findByCorreo("sakura@test.com")).thenReturn(Optional.of(usuarioEjemplo));

			assertThatThrownBy(() -> usuarioService.login("sakura@test.com", "claveIncorrecta"))
					.isInstanceOf(CredencialesInvalidasException.class)
					.hasMessage(UsuarioService.MENSAJE_CREDENCIALES);

			assertThat(usuarioEjemplo.getIntentosFallidos()).isEqualTo(1);
			assertThat(usuarioEjemplo.getUltimoIntento()).isNotNull();
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe denegar login si el usuario se encuentra en periodo de bloqueo")
		void login_UsuarioBloqueado_LanzaExcepcion() {
			usuarioEjemplo.setIntentosFallidos(UsuarioService.MAX_INTENTOS);
			usuarioEjemplo.setUltimoIntento(LocalDateTime.now().minusMinutes(5)); // Bloqueado hace 5 min (máx 15 min)

			when(usuarioRepository.findByCorreo("sakura@test.com")).thenReturn(Optional.of(usuarioEjemplo));

			assertThatThrownBy(() -> usuarioService.login("sakura@test.com", "password123"))
					.isInstanceOf(CredencialesInvalidasException.class)
					.hasMessage(UsuarioService.MENSAJE_CREDENCIALES);
		}
	}

	// -------------------------------------------------------------------------
	// LOGOUT Y BÚSQUEDA POR TOKEN
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para logout y sesión")
	class LogoutTests {

		@Test
		@DisplayName("Debe cerrar sesión correctamente anulando el token")
		void logout_Exito() {
			usuarioEjemplo.setToken("valid-token");
			when(usuarioRepository.findByToken("valid-token")).thenReturn(Optional.of(usuarioEjemplo));

			usuarioService.logout("valid-token");

			assertThat(usuarioEjemplo.getToken()).isNull();
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe lanzar RecursoNoEncontradoException en logout con token inexistente")
		void logout_TokenInexistente_LanzaExcepcion() {
			when(usuarioRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> usuarioService.logout("invalid-token"))
					.isInstanceOf(RecursoNoEncontradoException.class)
					.hasMessage("No hay una sesión activa con ese token");
		}

		@Test
		@DisplayName("Debe retornar usuario al buscar por token válido")
		void buscarPorToken_Exito() {
			usuarioEjemplo.setToken("valid-token");
			when(usuarioRepository.findByToken("valid-token")).thenReturn(Optional.of(usuarioEjemplo));

			Usuario resultado = usuarioService.buscarPorToken("valid-token");

			assertThat(resultado).isEqualTo(usuarioEjemplo);
		}
	}

	// -------------------------------------------------------------------------
	// RECUPERACIÓN Y RESTABLECIMIENTO DE CLAVE
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para recuperación de contraseña")
	class RecuperacionTests {

		@Test
		@DisplayName("Debe generar token de recuperación si el correo existe y está activo")
		void recuperar_UsuarioExiste_GeneraToken() {
			when(usuarioRepository.findByCorreo("sakura@test.com")).thenReturn(Optional.of(usuarioEjemplo));

			String mensaje = usuarioService.recuperar("sakura@test.com");

			assertThat(mensaje).isEqualTo(UsuarioService.MENSAJE_RECUPERAR);
			assertThat(usuarioEjemplo.getTokenRecuperacion()).isNotNull();
			assertThat(usuarioEjemplo.getTokenRecuperacionExpira()).isAfter(LocalDateTime.now());
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe retornar mensaje genérico pero no modificar nada si el correo no existe")
		void recuperar_UsuarioNoExiste_RetornaMensajeSinGuardar() {
			when(usuarioRepository.findByCorreo("nadie@test.com")).thenReturn(Optional.empty());

			String mensaje = usuarioService.recuperar("nadie@test.com");

			assertThat(mensaje).isEqualTo(UsuarioService.MENSAJE_RECUPERAR);
			verify(usuarioRepository, never()).save(any());
		}

		@Test
		@DisplayName("Debe restablecer contraseña con token válido y no expirado")
		void restablecer_Exito() {
			String token = "reset-token";
			usuarioEjemplo.setTokenRecuperacion(token);
			usuarioEjemplo.setTokenRecuperacionExpira(LocalDateTime.now().plusMinutes(10));
			usuarioEjemplo.setIntentosFallidos(3);

			when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.of(usuarioEjemplo));

			usuarioService.restablecer(token, "nuevaClave123");

			assertThat(usuarioEjemplo.getClave()).isEqualTo("nuevaClave123");
			assertThat(usuarioEjemplo.getTokenRecuperacion()).isNull();
			assertThat(usuarioEjemplo.getTokenRecuperacionExpira()).isNull();
			assertThat(usuarioEjemplo.getIntentosFallidos()).isZero();
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe lanzar ReglaNegocioException si el token de recuperación expiró")
		void restablecer_TokenExpirado_LanzaExcepcion() {
			String token = "token-expirado";
			usuarioEjemplo.setTokenRecuperacion(token);
			usuarioEjemplo.setTokenRecuperacionExpira(LocalDateTime.now().minusMinutes(1));

			when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.of(usuarioEjemplo));

			assertThatThrownBy(() -> usuarioService.restablecer(token, "nuevaClave123"))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El enlace de recuperación venció");
		}
	}

	// -------------------------------------------------------------------------
	// FUNCIONES DE ADMINISTRADOR
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para funciones de Admin")
	class AdminTests {

		private Usuario admin;

		@BeforeEach
		void setupAdmin() {
			admin = new Usuario();
			admin.setId(99L);
			admin.setRol("ADMIN");
			admin.setActivo(true);
			admin.setToken("admin-token");
		}

		@Test
		@DisplayName("Debe permitir a un ADMIN crear otro usuario ADMIN")
		void crearAdmin_Exito() {
			when(usuarioRepository.findByToken("admin-token")).thenReturn(Optional.of(admin));
			when(usuarioRepository.existsByCorreo("nuevo@admin.com")).thenReturn(false);
			when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

			Usuario nuevoAdmin = new Usuario();
			nuevoAdmin.setCorreo("nuevo@admin.com");

			Usuario resultado = usuarioService.crearAdmin("admin-token", nuevoAdmin);

			assertThat(resultado.getRol()).isEqualTo("ADMIN");
			assertThat(resultado.isActivo()).isTrue();
		}

		@Test
		@DisplayName("Debe lanzar NoAutenticadoException si el token no se provee")
		void crearAdmin_TokenNulo_LanzaExcepcion() {
			assertThatThrownBy(() -> usuarioService.crearAdmin(null, usuarioEjemplo))
					.isInstanceOf(NoAutenticadoException.class)
					.hasMessage("Debe iniciar sesión");
		}

		@Test
		@DisplayName("Debe lanzar AccesoDenegadoException si el usuario no tiene rol ADMIN")
		void crearAdmin_UsuarioSinRolAdmin_LanzaExcepcion() {
			usuarioEjemplo.setToken("cliente-token");
			when(usuarioRepository.findByToken("cliente-token")).thenReturn(Optional.of(usuarioEjemplo));

			assertThatThrownBy(() -> usuarioService.crearAdmin("cliente-token", new Usuario()))
					.isInstanceOf(AccesoDenegadoException.class)
					.hasMessage("No tiene permiso para esta acción");
		}

		@Test
		@DisplayName("Debe desactivar un usuario e inhabilitar su token activo")
		void cambiarActivo_DesactivarUsuario_Exito() {
			when(usuarioRepository.findByToken("admin-token")).thenReturn(Optional.of(admin));
			when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));
			when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

			usuarioEjemplo.setToken("session-token");

			Usuario resultado = usuarioService.cambiarActivo("admin-token", 1L, false);

			assertThat(resultado.isActivo()).isFalse();
			assertThat(resultado.getToken()).isNull();
		}
	}

	// -------------------------------------------------------------------------
	// ACTUALIZACIÓN DE PERFIL Y CAMBIO DE CLAVE
	// -------------------------------------------------------------------------
	@Nested
	@DisplayName("Pruebas para perfil y contraseña")
	class PerfilTests {

		@Test
		@DisplayName("Debe actualizar el perfil correctamente")
		void actualizarPerfil_Exito() {
			usuarioEjemplo.setToken("token-usuario");
			when(usuarioRepository.findByToken("token-usuario")).thenReturn(Optional.of(usuarioEjemplo));
			when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

			PerfilRequest request = new PerfilRequest();
			request.setNombre("Tomoyo");
			request.setApellido("Daidouji");
			request.setTelefono("987654321");

			Usuario resultado = usuarioService.actualizarPerfil("token-usuario", request);

			assertThat(resultado.getNombre()).isEqualTo("Tomoyo");
			assertThat(resultado.getApellido()).isEqualTo("Daidouji");
			assertThat(resultado.getTelefono()).isEqualTo("987654321");
		}

		@Test
		@DisplayName("Debe cambiar la clave exitosamente si la actual coincide")
		void cambiarClave_Exito() {
			usuarioEjemplo.setToken("token-usuario");
			when(usuarioRepository.findByToken("token-usuario")).thenReturn(Optional.of(usuarioEjemplo));

			CambioClaveRequest request = new CambioClaveRequest();
			request.setClaveActual("password123");
			request.setClaveNueva("newPassword123");

			usuarioService.cambiarClave("token-usuario", request);

			assertThat(usuarioEjemplo.getClave()).isEqualTo("newPassword123");
			verify(usuarioRepository).save(usuarioEjemplo);
		}

		@Test
		@DisplayName("Debe lanzar CredencialesInvalidasException en cambio de clave si la clave actual es errónea")
		void cambiarClave_ClaveActualErronea_LanzaExcepcion() {
			usuarioEjemplo.setToken("token-usuario");
			when(usuarioRepository.findByToken("token-usuario")).thenReturn(Optional.of(usuarioEjemplo));

			CambioClaveRequest request = new CambioClaveRequest();
			request.setClaveActual("incorrecta");
			request.setClaveNueva("newPassword123");

			assertThatThrownBy(() -> usuarioService.cambiarClave("token-usuario", request))
					.isInstanceOf(CredencialesInvalidasException.class)
					.hasMessage(UsuarioService.MENSAJE_CREDENCIALES);

			verify(usuarioRepository, never()).save(any());
		}

		@Test
		@DisplayName("Debe verificar clave correctamente")
		void verificarClave_Exito() {
			usuarioEjemplo.setToken("token-usuario");
			when(usuarioRepository.findByToken("token-usuario")).thenReturn(Optional.of(usuarioEjemplo));

			// No debe lanzar excepciones
			usuarioService.verificarClave("token-usuario", "password123");
		}
	}
}