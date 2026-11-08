package br.ufal.ava.curso.servico;

import br.ufal.ava.commons.events.CursoPublicadoEvento;
import br.ufal.ava.commons.events.EventoPublisher;
import br.ufal.ava.commons.events.Topicos;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import br.ufal.ava.commons.web.PaginaResposta;
import br.ufal.ava.commons.web.Paginacao;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import br.ufal.ava.curso.api.Dtos.*;
import br.ufal.ava.curso.dominio.*;
import br.ufal.ava.curso.dominio.AulaRepository.ContagemAulas;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CursoService {

    private final CursoRepository cursos;
    private final CategoriaRepository categorias;
    private final ModuloRepository modulos;
    private final AulaRepository aulas;
    private final MaterialRepository materiais;
    private final EventoPublisher eventos;

    public CursoService(CursoRepository cursos, CategoriaRepository categorias, ModuloRepository modulos,
                        AulaRepository aulas, MaterialRepository materiais, EventoPublisher eventos) {
        this.cursos = cursos;
        this.categorias = categorias;
        this.modulos = modulos;
        this.aulas = aulas;
        this.materiais = materiais;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<CursoResumoDTO> catalogo(String busca, Long categoriaId, int pagina, int tamanho) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        var page = cursos.buscar(StatusCurso.PUBLICADO, termo, categoriaId,
                Paginacao.de(pagina, tamanho, 50, Sort.by(Sort.Direction.DESC, "criadoEm")));
        Map<Long, Integer> totais = totaisDeAulas(page.getContent().stream().map(Curso::getId).toList());
        return PaginaResposta.de(page, c -> paraResumo(c, totais));
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> categorias() {
        return categorias.findAll(Sort.by("nome")).stream().map(CategoriaDTO::de).toList();
    }

    /** Rascunhos só aparecem para o professor responsável e para administradores. */
    @Transactional(readOnly = true)
    public CursoDetalheDTO detalhe(Long id, UsuarioAutenticado usuario) {
        Curso c = curso(id);
        exigirVisivel(c, usuario);
        return CursoDetalheDTO.de(c);
    }

    @Transactional(readOnly = true)
    public CursoResumoDTO resumo(Long id) {
        return paraResumo(curso(id));
    }

    @Transactional(readOnly = true)
    public List<CursoResumoDTO> resumos(Collection<Long> ids) {
        return resumosDe(cursos.findByIdIn(ids));
    }

    @Transactional(readOnly = true)
    public List<CursoResumoDTO> doProfessor(Long professorId) {
        return resumosDe(cursos.findByProfessorIdOrderByCriadoEmDesc(professorId));
    }

    /** Todos os cursos publicados, usado pelo recomendacao-service para montar o modelo de conteúdo. */
    @Transactional(readOnly = true)
    public List<CursoResumoDTO> catalogoCompleto() {
        return resumosDe(cursos.findByStatus(StatusCurso.PUBLICADO));
    }

    @Transactional(readOnly = true)
    public AulaDTO aula(Long id, UsuarioAutenticado usuario) {
        Aula a = aula(id);
        exigirVisivel(a.getModulo().getCurso(), usuario);
        return AulaDTO.de(a);
    }

    @Transactional(readOnly = true)
    public AulaDTO aulaInterna(Long id) {
        return AulaDTO.de(aula(id));
    }

    @Transactional(readOnly = true)
    public List<TrechoDTO> trechos(Long cursoId, String consulta, int limite) {
        curso(cursoId);
        return BuscaTrechos.buscar(aulas.listarPorCurso(cursoId), consulta, Math.clamp(limite, 1, 10));
    }

    @Transactional
    public CursoResumoDTO criar(Long professorId, CursoRequest req) {
        Categoria cat = categorias.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + req.categoriaId() + " não encontrada"));
        Curso c = cursos.save(new Curso(req.titulo().trim(), req.descricao(), cat, professorId));
        return paraResumo(c);
    }

    @Transactional
    public ModuloDTO adicionarModulo(Long professorId, Long cursoId, ModuloRequest req) {
        Curso c = cursoDoProfessor(cursoId, professorId);
        Modulo m = modulos.save(new Modulo(c, req.titulo().trim(), c.getModulos().size() + 1));
        return ModuloDTO.de(m);
    }

    @Transactional
    public AulaDTO criarAula(Long professorId, AulaRequest req) {
        Modulo m = modulos.findById(req.moduloId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo " + req.moduloId() + " não encontrado"));
        exigirDono(m.getCurso(), professorId);
        Aula a = aulas.save(new Aula(m, req.titulo().trim(), req.conteudo(), m.getAulas().size() + 1));
        return AulaDTO.de(a);
    }

    @Transactional
    public MaterialDTO adicionarMaterial(Long professorId, Long aulaId, MaterialRequest req) {
        Aula a = aula(aulaId);
        exigirDono(a.getModulo().getCurso(), professorId);
        return MaterialDTO.de(materiais.save(new Material(a, req.tipo(), req.titulo().trim(), req.url().trim())));
    }

    @Transactional
    public CursoResumoDTO publicar(Long professorId, Long cursoId) {
        Curso c = cursoDoProfessor(cursoId, professorId);
        if (aulas.contarPorCurso(cursoId) == 0) {
            throw new RegraNegocioException("Adicione ao menos uma aula antes de publicar o curso");
        }
        c.publicar();
        eventos.publicar(Topicos.CURSO_PUBLICADO, c.getId().toString(),
                new CursoPublicadoEvento(c.getId(), c.getTitulo(), c.getProfessorId(), Instant.now()));
        return paraResumo(c);
    }

    private List<CursoResumoDTO> resumosDe(List<Curso> lista) {
        Map<Long, Integer> totais = totaisDeAulas(lista.stream().map(Curso::getId).toList());
        return lista.stream().map(c -> paraResumo(c, totais)).toList();
    }

    private Map<Long, Integer> totaisDeAulas(List<Long> cursoIds) {
        if (cursoIds.isEmpty()) {
            return Map.of();
        }
        return aulas.contarPorCursos(cursoIds).stream()
                .collect(Collectors.toMap(ContagemAulas::cursoId, c -> c.total().intValue()));
    }

    private CursoResumoDTO paraResumo(Curso c) {
        return paraResumo(c, Map.of(c.getId(), aulas.contarPorCurso(c.getId())));
    }

    private static CursoResumoDTO paraResumo(Curso c, Map<Long, Integer> totais) {
        return new CursoResumoDTO(c.getId(), c.getTitulo(), c.getDescricao(), c.getCategoria().getNome(),
                c.getProfessorId(), c.getStatus(), totais.getOrDefault(c.getId(), 0));
    }

    private Curso curso(Long id) {
        return cursos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + id + " não encontrado"));
    }

    private Aula aula(Long id) {
        return aulas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Aula " + id + " não encontrada"));
    }

    private Curso cursoDoProfessor(Long cursoId, Long professorId) {
        Curso c = curso(cursoId);
        exigirDono(c, professorId);
        return c;
    }

    private static void exigirDono(Curso c, Long professorId) {
        if (!c.getProfessorId().equals(professorId)) {
            throw new AcessoNegadoException("Somente o professor responsável pode alterar este curso");
        }
    }

    /** 404 em vez de 403 para não revelar que o rascunho existe. */
    static void exigirVisivel(Curso c, UsuarioAutenticado usuario) {
        boolean visivel = c.getStatus() == StatusCurso.PUBLICADO
                || usuario.perfil() == Perfil.ADMIN
                || (usuario.perfil() == Perfil.PROFESSOR && c.getProfessorId().equals(usuario.id()));
        if (!visivel) {
            throw new RecursoNaoEncontradoException("Curso " + c.getId() + " não encontrado");
        }
    }
}
