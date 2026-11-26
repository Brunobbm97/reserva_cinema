package com.reservacinema.movie.controller;

import com.reservacinema.movie.dto.MovieRequest;
import com.reservacinema.movie.dto.MovieResponse;
import com.reservacinema.movie.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // ---- Leitura: pública (configurada como permitAll no SecurityConfig) ----

    @GetMapping("/api/movies")
    public List<MovieResponse> list() {
        return movieService.listAll();
    }

    @GetMapping("/api/movies/{id}")
    public MovieResponse get(@PathVariable Long id) {
        return movieService.getById(id);
    }

    // ---- Escrita: só ADMIN ----

    @PostMapping("/api/admin/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse create(@Valid @RequestBody MovieRequest request) {
        return movieService.create(request);
    }

    @PutMapping("/api/admin/movies/{id}")
    public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/api/admin/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        movieService.delete(id);
    }

    /**
     * Upload do pôster. É multipart/form-data (arquivo), não JSON.
     * O campo do formulário chama-se "file".
     */
    @PostMapping("/api/admin/movies/{id}/poster")
    public MovieResponse uploadPoster(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return movieService.updatePoster(id, file);
    }
}
