package br.ufal.ava.commons.events;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventoLeitor {

    private final ObjectMapper mapper;

    public EventoLeitor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T ler(String json, Class<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Evento inválido para " + tipo.getSimpleName() + ": " + json, e);
        }
    }
}
