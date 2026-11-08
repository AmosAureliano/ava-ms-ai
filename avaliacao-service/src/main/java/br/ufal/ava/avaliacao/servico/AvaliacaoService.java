package br.ufal.ava.avaliacao.servico;

import br.ufal.ava.avaliacao.api.Dtos.*;
import br.ufal.ava.avaliacao.cliente.CursoClient;
import br.ufal.ava.avaliacao.cliente.MatriculaClient;
import br.ufal.ava.avaliacao.dominio.*;
import br.ufal.ava.commons.events.EntregaAvaliadaEvento;
import br.ufal.ava.commons.events.EventoPublisher;
import br.ufal.ava.commons.events.Topicos;
import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.AcessoNegadoException;
import br.ufal.ava.commons.web.RecursoNaoEncontradoException;
import br.ufal.ava.commons.web.RegraNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AvaliacaoService {

    private final AtividadeRepository atividades;
    private final EntregaRepository entregas;
    private final CursoClient cursoClient;
    private final MatriculaClient matriculaClient;
    private final EventoPublisher eventos;
    private final ObjectMapper json;

    public AvaliacaoService(AtividadeRepository atividades, EntregaRepository entregas, CursoClient cursoClient,
                            MatriculaClient matriculaClient, EventoPublisher eventos, ObjectMapper json) {
        this.atividades = atividades;
        this.entregas = entregas;
        this.cursoClient = cursoClient;
        this.matriculaClient = matriculaClient;
        this.eventos = eventos;
        this.json = json;
    }

    @Transactional
    public AtividadeDTO criar(Long professorId, AtividadeRequest req) {
        var curso = cursoClient.buscar(req.cursoId());
        if (!professorId.equals(curso.professorId())) {
            throw new AcessoNegadoException("Somente o professor do curso pode criar atividades");
        }
        List<QuestaoRequest> questoes = req.questoes() == null ? List.of() : req.questoes();
        if (req.tipo() == TipoAtividade.QUESTIONARIO) {
            validarQuestionario(questoes);
        }
        Atividade a = new Atividade(req.cursoId(), professorId, req.titulo().trim(), req.descricao(), req.tipo(),
                req.prazo());
        if (req.tipo() == TipoAtividade.QUESTIONARIO) {
            for (QuestaoRequest qr : questoes) {
                Questao q = a.adicionarQuestao(qr.enunciado(), qr.peso() == null ? BigDecimal.ONE : qr.peso());
                qr.alternativas().forEach(alt -> q.adicionarAlternativa(alt.texto(), alt.correta()));
            }
        }
        return AtividadeDTO.de(atividades.save(a), true);
    }

    static void validarQuestionario(List<QuestaoRequest> questoes) {
        if (questoes.isEmpty()) {
            throw new RegraNegocioException("Questionário precisa de pelo menos uma questão");
        }
        for (int i = 0; i < questoes.size(); i++) {
            var q = questoes.get(i);
            long corretas = q.alternativas().stream().filter(AlternativaRequest::correta).count();
            if (q.alternativas().size() < 2 || corretas != 1) {
                throw new RegraNegocioException("Questão " + (i + 1) + ": informe ao menos 2 alternativas e exatamente 1 correta");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AtividadeResumoDTO> doCurso(Long cursoId, Long alunoId) {
        return resumos(atividades.findByCursoIdOrderByPrazo(cursoId), alunoId);
    }

    @Transactional(readOnly = true)
    public List<AtividadeResumoDTO> proximas(Collection<Long> cursoIds, Long alunoId) {
        if (cursoIds.isEmpty()) {
            return List.of();
        }
        return resumos(atividades.findTop10ByCursoIdInAndPrazoAfterOrderByPrazo(cursoIds, LocalDateTime.now()), alunoId);
    }

    /**
     * Professor responsável vê o gabarito; outro professor não acessa; aluno precisa estar matriculado
     * no curso; administrador vê a atividade sem gabarito.
     */
    @Transactional(readOnly = true)
    public AtividadeDTO detalhe(Long id, UsuarioAutenticado usuario) {
        Atividade a = atividade(id);
        boolean dono = usuario.perfil() == Perfil.PROFESSOR && a.getProfessorId().equals(usuario.id());
        switch (usuario.perfil()) {
            case PROFESSOR -> {
                if (!dono) {
                    throw new AcessoNegadoException("Atividade de um curso de outro professor");
                }
            }
            case ALUNO -> exigirMatricula(usuario.id(), a.getCursoId());
            case ADMIN -> { }
        }
        return AtividadeDTO.de(a, dono);
    }

    /** Registra a entrega; questionários são corrigidos na hora e publicam entrega.avaliada. */
    @Transactional
    public EntregaDTO entregar(Long alunoId, EntregaRequest req) {
        Atividade a = atividade(req.atividadeId());
        exigirMatricula(alunoId, a.getCursoId());
        if (LocalDateTime.now().isAfter(a.getPrazo())) {
            throw new RegraNegocioException("O prazo desta atividade já terminou");
        }
        if (entregas.existsByAtividadeIdAndAlunoId(a.getId(), alunoId)) {
            throw new RegraNegocioException("Você já enviou esta atividade");
        }
        String resposta = a.getTipo() == TipoAtividade.QUESTIONARIO ? paraJson(req.respostas()) : req.texto();
        if (resposta == null || resposta.isBlank()) {
            throw new RegraNegocioException("Resposta vazia");
        }
        Entrega e = entregas.saveAndFlush(new Entrega(a, alunoId, resposta));
        if (a.getTipo() == TipoAtividade.QUESTIONARIO) {
            e.avaliar(Correcao.corrigirObjetivas(a, req.respostas()), "Corrigido automaticamente");
            publicarAvaliacao(e);
        }
        return EntregaDTO.de(e);
    }

    @Transactional
    public EntregaDTO lancarNota(Long professorId, Long entregaId, NotaRequest req) {
        Entrega e = entregas.findById(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega " + entregaId + " não encontrada"));
        if (!e.getAtividade().getProfessorId().equals(professorId)) {
            throw new AcessoNegadoException("Somente o professor da atividade pode lançar a nota");
        }
        e.avaliar(req.nota(), req.comentario());
        publicarAvaliacao(e);
        return EntregaDTO.de(e);
    }

    @Transactional(readOnly = true)
    public List<EntregaDTO> doAluno(Long alunoId) {
        return entregas.findByAlunoIdOrderByEnviadaEmDesc(alunoId).stream().map(EntregaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EntregaDTO> daAtividade(Long professorId, Long atividadeId) {
        Atividade a = atividade(atividadeId);
        if (!a.getProfessorId().equals(professorId)) {
            throw new AcessoNegadoException("Somente o professor da atividade pode ver as entregas");
        }
        return entregas.findByAtividadeIdOrderByEnviadaEm(atividadeId).stream().map(EntregaDTO::de).toList();
    }

    private void exigirMatricula(Long alunoId, Long cursoId) {
        if (!matriculaClient.progresso(alunoId, cursoId).matriculado()) {
            throw new AcessoNegadoException("Matricule-se no curso para acessar esta atividade");
        }
    }

    private void publicarAvaliacao(Entrega e) {
        Atividade a = e.getAtividade();
        eventos.publicar(Topicos.ENTREGA_AVALIADA, String.valueOf(e.getId()),
                new EntregaAvaliadaEvento(e.getId(), a.getId(), a.getTitulo(), a.getCursoId(), e.getAlunoId(),
                        e.getNota(), Instant.now()));
    }

    private List<AtividadeResumoDTO> resumos(List<Atividade> lista, Long alunoId) {
        Set<Long> entregues = alunoId == null || lista.isEmpty() ? Set.of()
                : entregas.atividadesEntregues(alunoId, lista.stream().map(Atividade::getId).toList());
        return lista.stream()
                .map(a -> new AtividadeResumoDTO(a.getId(), a.getCursoId(), a.getTitulo(), a.getTipo(), a.getPrazo(),
                        a.getQuestoes().size(), alunoId == null ? null : entregues.contains(a.getId())))
                .toList();
    }

    private Atividade atividade(Long id) {
        return atividades.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade " + id + " não encontrada"));
    }

    private String paraJson(Map<Long, Long> respostas) {
        if (respostas == null || respostas.isEmpty()) {
            return null;
        }
        try {
            return json.writeValueAsString(respostas);
        } catch (JsonProcessingException ex) {
            throw new RegraNegocioException("Respostas inválidas");
        }
    }
}
