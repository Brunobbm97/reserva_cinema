package com.reservacinema.common.storage;

import org.springframework.web.multipart.MultipartFile;

/** Guarda e remove arquivos de pôster. Hoje grava no disco local (decisão D18). */
public interface PosterStorageService {

    /** Grava o arquivo e devolve o caminho relativo guardado no banco (ex.: "posters/uuid.jpg"). */
    String store(MultipartFile file);

    /** Remove o arquivo apontado por um caminho relativo. Não falha se já não existir. */
    void delete(String relativePath);
}
