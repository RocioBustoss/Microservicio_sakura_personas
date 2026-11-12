package com.sakura.personas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sakura.personas.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	boolean existsByCorreo(String correo);

	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByToken(String token);

	Optional<Usuario> findByTokenRecuperacion(String tokenRecuperacion);

}
