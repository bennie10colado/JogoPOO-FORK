package br.com.jogo.resources;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.jogo.domain.ConfiguracaoPartida;
import br.com.jogo.domain.RegistroPartida;
import br.com.jogo.dto.ConfiguracaoPartidaDTO;
import br.com.jogo.dto.ConfiguracaoPartidaNewDTO;
import br.com.jogo.dto.RegistroPartidaDTO;
import br.com.jogo.dto.RegistroPartidaNewDTO;
import br.com.jogo.facade.Jogo;

@RestController
@RequestMapping(path = "/partidas")
public class PartidaResource {
	@Autowired
	private Jogo jogo;

	@RequestMapping(value = "/configuracoes/{id}", method = RequestMethod.GET)
	public ResponseEntity<ConfiguracaoPartida> findConfiguracaoPartida(@PathVariable Integer id) {
		ConfiguracaoPartida obj = jogo.findConfiguracaoPartida(id);
		return ResponseEntity.ok().body(obj);
	}

	@RequestMapping(value = "/configuracoes", method = RequestMethod.POST)
	public ResponseEntity<Integer> insertConfiguracaoPartida(@Valid @RequestBody ConfiguracaoPartidaNewDTO objNewDto) {
		ConfiguracaoPartida obj = jogo.insertConfiguracaoPartida(objNewDto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getId()).toUri();
		return ResponseEntity.created(uri).body(obj.getId());
	}

	@RequestMapping(value = "/configuracoes/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteConfiguracaoPartida(@PathVariable Integer id) {
		jogo.deleteConfiguracaoPartida(id);
		return ResponseEntity.noContent().build();
	}

	@RequestMapping(value = "/configuracoes", method = RequestMethod.GET)
	public ResponseEntity<List<ConfiguracaoPartidaDTO>> findAllConfiguracaoPartidas() {
		List<ConfiguracaoPartidaDTO> list = jogo.findAllConfiguracaoPartidas();
		return ResponseEntity.ok().body(list);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Integer> insert(@Valid @RequestBody RegistroPartidaNewDTO objNewDto) {
		RegistroPartida obj = jogo.insertRegistroPartida(objNewDto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getId()).toUri();
		return ResponseEntity.created(uri).body(obj.getId());
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<RegistroPartida> findRegistroPartida(@PathVariable Integer id) {
		RegistroPartida obj = jogo.findRegistroPartida(id);
		return ResponseEntity.ok().body(obj);
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteRegistroPartida(@PathVariable Integer id) {
		jogo.deleteRegistroPartida(id);
		return ResponseEntity.noContent().build();
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<RegistroPartidaDTO>> findAllRegistroPartidas() {
		List<RegistroPartidaDTO> list = jogo.findAllRegistroPartidas();
		return ResponseEntity.ok().body(list);
	}
}