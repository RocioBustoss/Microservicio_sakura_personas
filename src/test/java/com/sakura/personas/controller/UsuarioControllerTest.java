package com.sakura.personas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.sakura.personas.exception.RecursoNoEncontradoException;
import com.sakura.personas.model.Usuario;
import com.sakura.personas.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@ActiveProfiles("test")
class UsuarioControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioService usuarioService;

	private static final String AUTH = "Bearer token-admin";
	private static final String USUARIO = "{\"nombre\":\"Ana\",\"correo\":\"ana@test.com\",\"clave\":\"secreto1\"}";

	@Test
	void sesion_Exito() throws Exception {
		when(usuarioService.buscarPorToken("token-1")).thenReturn(usuario(3L));

		mockMvc.perform(get("/api/v1/usuarios/sesion/token-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.correo").value("ana@test.com"));
	}

	@Test
	void sesion_NoExiste_Responde404() throws Exception {
		when(usuarioService.buscarPorToken("nadie")).thenThrow(new RecursoNoEncontradoException("La sesión no es válida"));

		mockMvc.perform(get("/api/v1/usuarios/sesion/nadie"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("La sesión no es válida"));
	}

	@Test
	void crearAdmin_Exito() throws Exception {
		when(usuarioService.crearAdmin(eq("token-admin"), any(Usuario.class))).thenReturn(usuario(9L));

		mockMvc.perform(post("/api/v1/usuarios")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(USUARIO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(9));
	}

	@Test
	void crearAdmin_SinToken_Responde401() throws Exception {
		mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void perfil_Exito() throws Exception {
		when(usuarioService.actualizarPerfil(eq("token-admin"), any())).thenReturn(usuario(3L));

		mockMvc.perform(put("/api/v1/usuarios/perfil")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Tomoyo\",\"apellido\":\"Daidouji\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Ana"));
	}

	@Test
	void clave_Exito() throws Exception {
		mockMvc.perform(put("/api/v1/usuarios/clave")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"claveActual\":\"secreto1\",\"claveNueva\":\"nueva123\",\"confirmacion\":\"nueva123\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string("La contraseña fue actualizada"));
	}

	@Test
	void clave_ConfirmacionDistinta_Responde400() throws Exception {
		mockMvc.perform(put("/api/v1/usuarios/clave")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"claveActual\":\"secreto1\",\"claveNueva\":\"nueva123\",\"confirmacion\":\"otra123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La confirmación no coincide con la clave nueva"));
	}

	@Test
	void clave_ActualIncorrecta_Responde401() throws Exception {
		doThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"))
				.when(usuarioService).cambiarClave(eq("token-admin"), any());

		mockMvc.perform(put("/api/v1/usuarios/clave")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"claveActual\":\"mala\",\"claveNueva\":\"nueva123\",\"confirmacion\":\"nueva123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void activo_Exito() throws Exception {
		Usuario desactivado = usuario(3L);
		desactivado.setActivo(false);
		when(usuarioService.cambiarActivo("token-admin", 3L, false)).thenReturn(desactivado);

		mockMvc.perform(put("/api/v1/usuarios/3/activo")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"activo\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));
	}

	private Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre("Ana");
		usuario.setCorreo("ana@test.com");
		usuario.setActivo(true);
		return usuario;
	}
}
