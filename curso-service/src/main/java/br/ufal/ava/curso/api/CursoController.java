package br.ufal.ava.curso.api;

import br.ufal.ava.commons.security.Perfil;
import br.ufal.ava.commons.security.UsuarioAutenticado;
import br.ufal.ava.commons.web.Acesso;
import br.ufal.ava.commons.web.PaginaResposta;
import br.ufal.ava.curso.api.Dtos.*;
import br.ufal.ava.curso.servico.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService service;

    public CursoController(CursoService service) {
        this.service = service;
    }

    /** Catálogo público de cursos publicados (rota pública no gateway). */
    @GetMapping("/publicos")
    public PaginaResposta<CursoResumoDTO> catalogo(@RequestParam(required = false) String busca,
                                                   @RequestParam(required = false) Long categoriaId,
                                                   @RequestParam(defaultValue = "0") int pagina,
                                                   @RequestParam(defaultValue = "12") int tamanho) {
        return service.catalogo(busca, categoriaId, pagina, tamanho);
    }

    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return service.categorias();
    }

    /** Cursos do professor logado (inclusive rascunhos). */
    @GetMapping("/meus")
    public List<CursoResumoDTO> meus(UsuarioAutenticado usuario) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.doProfessor(usuario.id());
    }

    @GetMapping("/{id}")
    public CursoDetalheDTO detalhe(UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.detalhe(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResumoDTO criar(UsuarioAutenticado usuario, @Valid @RequestBody CursoRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.criar(usuario.id(), req);
    }

    @PostMapping("/{id}/modulos")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloDTO adicionarModulo(UsuarioAutenticado usuario, @PathVariable Long id,
                                     @Valid @RequestBody ModuloRequest req) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.adicionarModulo(usuario.id(), id, req);
    }

    /** Publica o curso e emite o evento curso.publicado. */
    @PatchMapping("/{id}/publicar")
    public CursoResumoDTO publicar(UsuarioAutenticado usuario, @PathVariable Long id) {
        Acesso.exigir(usuario, Perfil.PROFESSOR);
        return service.publicar(usuario.id(), id);
    }
}
