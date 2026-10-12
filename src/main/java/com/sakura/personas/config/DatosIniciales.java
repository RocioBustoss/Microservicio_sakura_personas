package com.sakura.personas.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sakura.personas.model.Usuario;
import com.sakura.personas.repository.UsuarioRepository;

@Component
public class DatosIniciales implements CommandLineRunner {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Override
	public void run(String... args) {
		if (usuarioRepository.count() > 0) {
			return;
		}
		Usuario admin = new Usuario();
		admin.setNombre("Admin");
		admin.setApellido("Sakura");
		admin.setCorreo("admin@sakura.cl");
		admin.setClave("admin123");
		admin.setRol("ADMIN");
		admin.setActivo(true);
		admin.setIntentosFallidos(0);
		usuarioRepository.save(admin);
	}

}
