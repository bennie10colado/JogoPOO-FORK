package br.com.jogo.facade;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import br.com.jogo.domain.Admin;
import br.com.jogo.domain.Alternativa;
import br.com.jogo.domain.Categoria;
import br.com.jogo.domain.ConfiguracaoPartida;
import br.com.jogo.domain.Item;
import br.com.jogo.domain.Jogador;
import br.com.jogo.domain.Questao;
import br.com.jogo.domain.RegistroPartida;
import br.com.jogo.domain.Usuario;
import br.com.jogo.domain.enums.Role;
import br.com.jogo.security.UserSS;
import br.com.jogo.security.exceptions.AuthorizationException;
import br.com.jogo.security.exceptions.InvalidRoleUser;
import br.com.jogo.security.exceptions.InvalidTokenException;
import br.com.jogo.services.AdminService;
import br.com.jogo.services.AlternativaService;
import br.com.jogo.services.AuthService;
import br.com.jogo.services.CategoriaService;
import br.com.jogo.services.ConfiguracaoPartidaService;
import br.com.jogo.services.EmailService;
import br.com.jogo.services.ImageService;
import br.com.jogo.services.ItemService;
import br.com.jogo.services.JogadorService;
import br.com.jogo.services.QuestaoService;
import br.com.jogo.services.RegistroPartidaService;
import br.com.jogo.services.UserService;
import br.com.jogo.services.UsuarioService;
import br.com.jogo.services.exceptions.ActivationException;
import br.com.jogo.services.exceptions.IncorrectAlternativeException;
import br.com.jogo.services.exceptions.InvalidNextQuestionException;
import br.com.jogo.services.exceptions.ObjectNotFoundException;

@Component
public class Jogo {
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private AdminService adminService;
	@Autowired
	private AlternativaService alternativaService;
	@Autowired
	private CategoriaService categoriaService;
	@Autowired
	private ConfiguracaoPartidaService configuracaoPartidaService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private JogadorService jogadorService;
	@Autowired
	private QuestaoService questaoService;
	@Autowired
	private RegistroPartidaService registroPartidaService;
	@Autowired
	private BCryptPasswordEncoder pe;
	@Autowired
	private ImageService imageService;
	@Autowired
	private EmailService emailService;
	@Autowired
	private AuthService authService;

	@Value("${domain.url}")
	private String domainURL;

	// --------------------------------Admin----------------------------------------------

	public Admin findAdmin(Integer id) throws AuthorizationException {
		UserSS userss = UserService.authenticated();
		if (userss == null || !userss.hasRole(Role.ADMIN) && !id.equals(userss.getId())) {
			throw new AuthorizationException("Acesso negado!");
		}
		return adminService.find(id);
	}

	public Admin insertAdmin(Admin obj) {
		obj.setSenha(pe.encode(obj.getSenha()));
		return adminService.insert(obj);
	}

	public Admin updateAdmin(Admin obj) {
		return adminService.update(obj);
	}

	public void deleteAdmin(Integer id) {
		adminService.delete(id);
	}

	public Admin findAdminByEmail(String email) {
		Usuario user = findUsuarioByEmail(email);
		if (!(user instanceof Admin)) {
			throw new AuthorizationException("Acesso negado");
		}
		return (Admin) user;
	}

	public List<Admin> findAllAdmins() {
		return adminService.findAll();
	}

	// --------------------------------Alternativa----------------------------------------------

	private Alternativa findAlternativa(Integer id) {
		return alternativaService.find(id);
	}

	// --------------------------------Categoria----------------------------------------------

	public Categoria insertCategoria(Categoria obj) {
		return categoriaService.insert(obj);
	}

	public Categoria findCategoria(Integer id) {
		return categoriaService.find(id);
	}

	public List<Categoria> findAllCategorias() {
		return categoriaService.findAll();
	}

	public void deleteCategoria(Integer id) {
		categoriaService.delete(id);
	}

	public Categoria updateCategoria(Categoria obj) {
		return categoriaService.update(obj);
	}

	// --------------------------------ConfiguracaoPartida----------------------------------------------
	public ConfiguracaoPartida insertConfiguracaoPartida(ConfiguracaoPartida obj)
			throws AuthorizationException, InvalidRoleUser, ObjectNotFoundException {
		UserSS userss = UserService.authenticated();
		if (userss == null) {
			throw new AuthorizationException("Acesso negado!");
		} else {
			if (!userss.hasRole(Role.JOGADOR)) {
				throw new InvalidRoleUser("Apenas jogadores podem iniciar uma partida!");
			}
		}
		Jogador jog = findJogador(userss.getId());
		obj.setJogador(jog);
		if (obj.getQuestoes() != null && !obj.getQuestoes().isEmpty()) {
			obj = new ConfiguracaoPartida(jog,
					obj.getQuestoes().stream().map(x -> questaoService.find(x.getId())).collect(Collectors.toSet()));
		} else {
			obj = new ConfiguracaoPartida(jog, obj.getNivel());
			if (obj.getCategorias() != null && !obj.getCategorias().isEmpty()) {
				obj.setCategorias(obj.getCategorias().stream().map(x -> categoriaService.find(x.getId()))
						.collect(Collectors.toSet()));
			}
			Questao nextQ = nextQuestionConfiguracaoPartida(obj);
			if (nextQ == null) {
				throw new ObjectNotFoundException("Não há questões para essa configuração de partida!");
			}
			obj.setQuestoes(Set.of(nextQ));
		}
		return configuracaoPartidaService.insert(obj);
	}

	public ConfiguracaoPartida findConfiguracaoPartida(Integer id) {
		return configuracaoPartidaService.find(id);
	}

	public List<ConfiguracaoPartida> findAllConfiguracaoPartidas() {
		return configuracaoPartidaService.findAll();
	}
	
	public List<ConfiguracaoPartida> findConfiguracaoPartidasPreseteds() {
		return configuracaoPartidaService.findPreseteds();
	}

	public void deleteConfiguracaoPartida(Integer id) {
		configuracaoPartidaService.delete(id);
	}

	// --------------------------------Item----------------------------------------------

	public Item findItem(Integer id) {
		return itemService.find(id);
	}

	public Item updateItem(Item obj) {
		return itemService.update(obj);
	}

	public void deleteItem(Integer id) {
		itemService.delete(id);
	}

	public List<Item> findAllItens() {
		return itemService.findAll();
	}

	// --------------------------------Jogador----------------------------------------------

	public Jogador findJogador(Integer id) {
		return jogadorService.find(id);
	}

	public Jogador insertJogador(Jogador obj) {
		obj.setSenha(pe.encode(obj.getSenha()));
		emailService.sendUserConfirmationHtmlEmail(obj);
		return jogadorService.insert(obj);
	}

	public Jogador updateJogador(Jogador obj) {
		return jogadorService.update(obj);
	}

	public void deleteJogador(Integer id) {
		jogadorService.delete(id);
	}

	public List<Jogador> findAllJogadores() {
		return jogadorService.findAll();
	}

	public List<Jogador> rankJogadores() {
		return jogadorService.rank();
	}

	public Jogador findJogadorByEmail(String email) {
		Usuario user = findUsuarioByEmail(email);
		if (!(user instanceof Jogador)) {
			throw new AuthorizationException("Acesso negado");
		}
		return (Jogador) user;

	}

	public URI uploadProfilePictureOfJogador(MultipartFile multipartFile) throws AuthorizationException {
		UserSS user = UserService.authenticated();
		if (user == null || !user.hasRole(Role.JOGADOR)) {
			throw new AuthorizationException("Acesso negado");
		}
		BufferedImage jpgImg = imageService.getJpgImageFromFile(multipartFile);
		jpgImg = imageService.cropSquare(jpgImg);
		jpgImg = imageService.resize(jpgImg);
		String fileName = "jog" + user.getId() + ".jpg";
		return imageService.uploadImage(imageService.getInputStream(jpgImg, "jpg"), fileName);
	}

	public void deleteProfilePictureOfJogador() {
		UserSS user = UserService.authenticated();
		if (user == null) {
			throw new AuthorizationException("Acesso negado");
		}
		imageService.deleteImage("jog" + user.getId() + ".jpg");
	}

	// --------------------------------Questao----------------------------------------------

	public Questao insertQuestao(Questao obj) {
		obj.setCategoria(categoriaService.find(obj.getCategoria().getId()));
		return questaoService.insert(obj);
	}

	public Questao findQuestao(Integer id) {
		return questaoService.find(id);
	}

	public List<Questao> findAllQuestoes() {
		return questaoService.findAll();
	}

	public void deleteQuestao(Integer id) {
		questaoService.delete(id);
	}

	public Questao updateQuestao(Questao obj) {
		obj.setCategoria(categoriaService.find(obj.getCategoria().getId()));
		List<Alternativa> alts = obj.getAlternativas().stream().toList();
		if (alts != null) {
			obj.setAlternativas(alternativaService.updateAllByList(findQuestao(obj.getId()).getAlternativas(), alts));
		}
		return questaoService.update(obj);
	}

	// -------------------------------------RegistroPartida-----------------------------------------

	public RegistroPartida insertRegistroPartida(RegistroPartida obj) throws AuthorizationException, InvalidRoleUser {
		UserSS userss = UserService.authenticated();
		if (userss == null) {
			throw new AuthorizationException("Acesso negado!");
		} else {
			if (!userss.hasRole(Role.JOGADOR)) {
				throw new InvalidRoleUser("Apenas jogadores podem iniciar uma partida!");
			}
		}
		Questao ultima = null;
		Jogador jog = jogadorService.find(userss.getId());
		if (obj.getConfiguracaoPartida() != null) {
			ConfiguracaoPartida cp = findConfiguracaoPartida(obj.getConfiguracaoPartida().getId());
			ultima = cp.getQuestoes().stream().findAny().get();
			obj = new RegistroPartida(cp, jog);

		} else {
			ConfiguracaoPartida cp = new ConfiguracaoPartida(jog);
			ultima = nextQuestionConfiguracaoPartida(cp);
			cp.setQuestoes(Set.of(ultima));
			obj = new RegistroPartida(cp, jog);
		}
		obj.setUltimaQuestao(ultima);
		return registroPartidaService.insert(obj);
	}

	public Questao UltimaQuestaoRegistroPartida(Integer id) throws ActivationException {
		RegistroPartida obj = findRegistroPartida(id);
		if (!obj.isAtiva()) {
			throw new ActivationException("A partida está inativa!");
		}
		return obj.getUltimaQuestao();
	}

	public RegistroPartida findRegistroPartida(Integer id) {
		return registroPartidaService.find(id);
	}

	public void deleteRegistroPartida(Integer id) {
		registroPartidaService.delete(id);
	}

	public List<RegistroPartida> findAllRegistroPartidas() {
		return registroPartidaService.findAll();
	}

	public List<RegistroPartida> findActiveByJogador(Jogador obj) {
		obj = findJogador(obj.getId());
		return registroPartidaService.findActiveByJogador(obj);
	}

	public List<RegistroPartida> rankRegistroPartidaByConfiguracaoPartida(ConfiguracaoPartida obj) {
		obj = configuracaoPartidaService.find(obj.getId());
		return registroPartidaService.rankByConfiguracaoPartida(obj);
	}

	public List<RegistroPartida> rankRegistroPartida() {
		return registroPartidaService.rank();
	}

	// -----------------------------Usuario-----------------------------

	private Usuario findUsuarioByEmail(String email) throws AuthorizationException {
		UserSS user = UserService.authenticated();
		if (user == null || !user.getUsername().equals(email)) {
			throw new AuthorizationException("Acesso negado");
		}
		return usuarioService.findByEmail(email);
	}

	// -----------------------------Autenticação-----------------------------
	public void sendRecoveryPassword(String email) {
		Usuario obj = usuarioService.findByEmail(email);
		String password = pe.encode(authService.newRandonPassword());
		usuarioService.updatePassword(obj, password);
		emailService.sendPassworRecoveyURLHtmlEmail(obj, authService.generateRecoveryPasswordUrl(obj, password));
	}

	public void insertNewPassword(String password, String token) throws InvalidTokenException {
		String[] usernamePassword = authService.recoverEmailAndPasswordbyToken(token);
		Usuario obj = usuarioService.findByEmail(usernamePassword[0]);
		if (!obj.getSenha().equals(usernamePassword[1])) {
			throw new InvalidTokenException("A senha já foi trocada");
		}
	}

	public Usuario updateSenha(String novaSenha, String senha) throws AuthorizationException {
		UserSS userss = UserService.authenticated();
		Usuario usuario;
		if (userss == null) {
			throw new AuthorizationException("Acesso negado");
		}
		if (userss.hasRole(Role.JOGADOR)) {
			usuario = findJogador(userss.getId());
		} else {
			usuario = findAdmin(userss.getId());
		}

		if (!pe.matches(senha, usuario.getSenha())) {
			throw new AuthorizationException("Senha incorreta");
		}
		usuario.setSenha(pe.encode(novaSenha));
		if (userss.hasRole(Role.JOGADOR)) {
			return updateJogador((Jogador) usuario);
		} else {
			return updateAdmin((Admin) usuario);
		}
	}

	// -----------------------------Regras Jogo-----------------------------

	public void answerQuestion(RegistroPartida registroPartida, Alternativa alternativa) throws AuthorizationException,
			ObjectNotFoundException, ActivationException, IncorrectAlternativeException, InvalidNextQuestionException {
		UserSS userss = UserService.authenticated();
		if (userss == null) {
			throw new AuthorizationException("Acesso negado!");
		} else {
			if (!userss.hasRole(Role.JOGADOR)) {
				throw new InvalidRoleUser("Apenas jogadores podem iniciar uma partida!");
			}
		}
		RegistroPartida rp = findRegistroPartida(registroPartida.getId());
		if (rp.getJogador().getId() != userss.getId()) {
			throw new AuthorizationException("Apenas o jogador da partida pode responder!");
		}
		if (!rp.isAtiva()) {
			throw new ActivationException("A partida está inativa!");
		}
		Alternativa a = findAlternativa(alternativa.getId());
		Questao q = questaoService.findByAlternativa(a);
		if (!rp.getUltimaQuestao().equals(q)) {
			throw new ObjectNotFoundException("A alternativa não pertece a uma questao da partida!");
		}
		if (a.isCorreta()) {
			rp = correctAnswer(rp, q);
		} else {
			rp.getJogador().addQtdPartidas();
			rp.setAtiva(false);
			if (!rp.getConfiguracaoPartida().isPredefinida())
				rp.getConfiguracaoPartida().toPreseted();
		}
		rp = registroPartidaService.update(rp);
		configuracaoPartidaService.update(rp.getConfiguracaoPartida());
		if (!a.isCorreta()) {
			throw new IncorrectAlternativeException(q.getCorrectAlternative());
		} else {
			if (rp.getUltimaQuestao() == null) {
				throw new InvalidNextQuestionException("Não há questoes disponiveis para o jogo!");
			}
		}
	}

	private RegistroPartida correctAnswer(RegistroPartida rp, Questao q) {
		rp.addPontuacao(q.getNivel());
		rp.getJogador().addPontuacao(q.getNivel());
		rp.getJogador().addSaldo(q.getNivel());
		rp.addQuestoes(q);
		Questao nextQ = nextQuestionRegistroPartida(rp);
		if (nextQ == null) {
			rp.getJogador().addQtdPartidas();
			if (!rp.getConfiguracaoPartida().isPredefinida())
				rp.getConfiguracaoPartida().toPreseted();
			rp.setAtiva(false);
		} else {
			if (!rp.getConfiguracaoPartida().isPredefinida()) {
				rp.getConfiguracaoPartida().addQuestao(nextQ);
			}
		}
		rp.setUltimaQuestao(nextQ);
		return rp;
	}

	private Questao nextQuestionRegistroPartida(RegistroPartida obj) throws InvalidNextQuestionException {
		ConfiguracaoPartida cp = obj.getConfiguracaoPartida();
		Questao nextQ = null;
		if (!cp.isPredefinida()) {
			nextQ = nextQuestionConfiguracaoPartida(cp);
		} else {
			Set<Questao> sq = cp.getQuestoes();
			sq.removeAll(obj.getQuestoesRepondias());
			if (!sq.isEmpty()) {
				nextQ = sq.stream().findAny().get();
			} else {
				throw new InvalidNextQuestionException("Não ha questoes disponiveis para o jogo!"); // endgame?
			}
		}
		return nextQ;
	}

	private Questao nextQuestionConfiguracaoPartida(ConfiguracaoPartida obj) {
		Questao nextQ = null;
		try {
			if (obj.getNivel() != 0 && obj.getCategorias() != null) {
				nextQ = questaoService.findOneByNivelAndCategoria(obj.getNivel(), obj.getCategorias(),
						obj.getQuestoes());
			} else if (obj.getNivel() != 0) {
				nextQ = questaoService.findOneByNivel(obj.getNivel(), obj.getQuestoes());
			} else if (obj.getCategorias() != null) {
				nextQ = questaoService.findOneByCategoria(obj.getCategorias(), obj.getQuestoes());
			} else {
				nextQ = questaoService.findOneNotIn(obj.getQuestoes());
			}
		} catch (ObjectNotFoundException e) {
			return nextQ;
		}
		return nextQ;
	}

	public List<RegistroPartida> findPartidaAtivaByJogador(Jogador obj) {
		return registroPartidaService.findActiveByJogador(obj);
	}

}
