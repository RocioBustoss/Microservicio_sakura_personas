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
		Usuario admin = usuarioRepository.findByCorreo("admin@sakura.cl").orElse(null);
		if (admin == null) {
			admin = new Usuario();
			admin.setNombre("Admin");
			admin.setApellido("Sakura");
			admin.setCorreo("admin@sakura.cl");
			admin.setIntentosFallidos(0);
		}
		admin.setClave("Sakura1&");
		admin.setRol("ADMIN");
		admin.setActivo(true);
		usuarioRepository.save(admin);
	}

}
