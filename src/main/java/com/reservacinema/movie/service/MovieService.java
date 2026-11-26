package com.reservacinema.movie.service;

import com.reservacinema.movie.dto.MovieRequest;
import com.reservacinema.movie.dto.MovieResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MovieService {

    MovieResponse create(MovieRequest request);

    MovieResponse update(Long id, MovieRequest request);

    void delete(Long id);

    List<MovieResponse> listAll();

    MovieResponse getById(Long id);

    /** Substitui o pôster do filme por um novo arquivo. */
    MovieResponse updatePoster(Long id, MultipartFile file);
}
