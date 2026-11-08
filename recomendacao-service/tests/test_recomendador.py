from app.dados import interpretar_evento
from app.recomendador import RecomendadorHibrido

CURSOS = [
    {"id": 1, "titulo": "Banco de Dados Relacional", "descricao": "Modelo ER, normalização e SQL no MySQL", "categoria": "Dados"},
    {"id": 2, "titulo": "MySQL na prática", "descricao": "Consultas SQL, índices e normalização", "categoria": "Dados"},
    {"id": 3, "titulo": "Spring Cloud", "descricao": "Microsserviços com gateway e Eureka", "categoria": "Arquitetura"},
    {"id": 4, "titulo": "React para iniciantes", "descricao": "Componentes, estado e hooks", "categoria": "Front-end"},
]


def test_partida_a_frio_retorna_populares():
    m = RecomendadorHibrido()
    m.treinar(CURSOS, [(10, 3, 1.0), (11, 3, 1.0), (11, 4, 1.0)])
    r = m.recomendar(99, 2)
    assert [x["cursoId"] for x in r] == [3, 4]
    assert r[0]["motivo"] == "Popular no AVA"


def test_conteudo_recomenda_curso_parecido_e_nao_repete():
    m = RecomendadorHibrido()
    m.treinar(CURSOS, [(10, 1, 1.0)])
    r = m.recomendar(10, 3)
    ids = [x["cursoId"] for x in r]
    assert 1 not in ids                      # já feito
    assert ids[0] == 2                       # MySQL é o mais parecido com Banco de Dados
    assert r[0]["titulo"] == "MySQL na prática"


def test_colaborativo_alunos_parecidos():
    m = RecomendadorHibrido()
    # alunos 1 e 2 fizeram React e Spring; aluno 3 só React -> Spring deve vir antes dos cursos de dados
    m.treinar(CURSOS, [(1, 4, 1.0), (1, 3, 1.0), (2, 4, 1.0), (2, 3, 1.0), (3, 4, 1.0)])
    r = m.recomendar(3, 1)
    assert r[0]["cursoId"] == 3


def test_sem_cursos_retorna_lista_vazia():
    m = RecomendadorHibrido()
    m.treinar([], [])
    assert m.recomendar(1) == []


def test_interpretar_eventos():
    assert interpretar_evento("matricula.criada", b'{"alunoId":1,"cursoId":2}') == (1, 2, 1.0, "MATRICULA")
    assert interpretar_evento("entrega.avaliada", b'{"alunoId":1,"cursoId":2,"nota":6.5}') is None
    assert interpretar_evento("entrega.avaliada", b'{"alunoId":1,"cursoId":2,"nota":9}') == (1, 2, 1.0, "NOTA")
    assert interpretar_evento("curso.publicado", b'{"cursoId":2}') is None
