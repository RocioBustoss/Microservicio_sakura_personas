package com.sakura.personas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sakura.personas.exception.CredencialesInvalidasException;
import com.sakura.personas.exception.NoAutenticadoException;
import com.sakura.personas.exception.ReglaNegocioException;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.service.UsuarioService;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioService usuarioService;

	private static final String USUARIO = "{\"nombre\":\"Sakura\",\"correo\":\"sakura@test.com\",\"clave\":\"secreto1\"}";

	@Test
	void registro_Exito() throws Exception {
		when(usuarioService.registrar(any(Usuario.class))).thenReturn(usuario(1L));

		mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(USUARIO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.correo").value("sakura@test.com"));
	}

	@Test
	void registro_CuerpoVacio_Responde400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registro_CorreoDuplicado_Responde409() throws Exception {
		when(usuarioService.registrar(any(Usuario.class)))
				.thenThrow(new ReglaNegocioException("El correo ya está registrado"));

		mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(USUARIO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
	}

	@Test
	void login_Exito() throws Exception {
		when(usuarioService.login("sakura@test.com", "secreto1")).thenReturn(usuario(1L));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"sakura@test.com\",\"clave\":\"secreto1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void login_ClaveIncorrecta_Responde401() throws Exception {
		when(usuarioService.login(any(), any())).thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"sakura@test.com\",\"clave\":\"mala\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
	}

	@Test
	void logout_Exito() throws Exception {
		doNothing().when(usuarioService).logout("token-1");

		mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer token-1"))
				.andExpect(status().isOk())
				.andExpect(content().string("Sesión cerrada"));
	}

	@Test
	void logout_SinToken_Responde401() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void recuperar_Exito() throws Exception {
		when(usuarioService.recuperar("sakura@test.com")).thenReturn(UsuarioService.MENSAJE_RECUPERAR);

		mockMvc.perform(post("/api/v1/auth/recuperar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"sakura@test.com\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string(UsuarioService.MENSAJE_RECUPERAR));
	}

	@Test
	void restablecer_Exito() throws Exception {
		doNothing().when(usuarioService).restablecer("reset", "nueva123");

		mockMvc.perform(post("/api/v1/auth/restablecer")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"reset\",\"clave\":\"nueva123\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string("La contraseña fue actualizada"));
	}

	@Test
	void verificar_Exito() throws Exception {
		doNothing().when(usuarioService).verificarClave(eq("token-1"), eq("secreto1"));

		mockMvc.perform(post("/api/v1/auth/verificar")
				.header("Authorization", "Bearer token-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clave\":\"secreto1\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string("Clave verificada"));
	}

	@Test
	void verificar_ClaveInvalida_Responde401() throws Exception {
		doThrow(new NoAutenticadoException("Debe iniciar sesión")).when(usuarioService).verificarClave(any(), any());

		mockMvc.perform(post("/api/v1/auth/verificar")
				.header("Authorization", "Bearer token-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"clave\":\"mala\"}"))
				.andExpect(status().isUnauthorized());
	}

	private Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre("Sakura");
		usuario.setCorreo("sakura@test.com");
		usuario.setClave("secreto1");
		return usuario;
	}
}
