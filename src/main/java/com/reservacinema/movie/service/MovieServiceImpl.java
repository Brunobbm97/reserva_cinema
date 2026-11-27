package com.reservacinema.movie.service;

import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.common.storage.PosterStorageService;
import com.reservacinema.movie.dto.MovieRequest;
import com.reservacinema.movie.dto.MovieResponse;
import com.reservacinema.movie.entity.Genre;
import com.reservacinema.movie.entity.Movie;
import com.reservacinema.movie.repository.GenreRepository;
import com.reservacinema.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final PosterStorageService posterStorage;

    public MovieServiceImpl(MovieRepository movieRepository,
                            GenreRepository genreRepository,
                            PosterStorageService posterStorage) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.posterStorage = posterStorage;
    }

    @Override
    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie(request.title().trim(), request.description(), request.durationMinutes());
        movie.setGenres(resolveGenres(request.genresOrEmpty()));
        movieRepository.save(movie);
        return MovieResponse.from(movie);
    }

    @Override
    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = findMovie(id);
        movie.setTitle(request.title().trim());
        movie.setDescription(request.description());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setGenres(resolveGenres(request.genresOrEmpty()));
        return MovieResponse.from(movie); // dirty checking salva ao fim da transação
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Movie movie = findMovie(id);
        String poster = movie.getPosterPath();
        movieRepository.delete(movie);
        posterStorage.delete(poster); // remove o arquivo do disco também
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> listAll() {
        return movieRepository.findAll().stream().map(MovieResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getById(Long id) {
        return MovieResponse.from(findMovie(id));
    }

    @Override
    @Transactional
    public MovieResponse updatePoster(Long id, MultipartFile file) {
        Movie movie = findMovie(id);
        String oldPoster = movie.getPosterPath();
        movie.setPosterPath(posterStorage.store(file));
        posterStorage.delete(oldPoster); // apaga o pôster anterior, se havia
        return MovieResponse.from(movie);
    }

    /**
     * Converte uma lista de NOMES de gênero em entidades Genre, reaproveitando os que já
     * existem (sem diferenciar maiúsculas) e criando os novos. Evita gêneros duplicados.
     */
    private Set<Genre> resolveGenres(List<String> names) {
        Set<Genre> result = new HashSet<>();
        for (String raw : names) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            Genre genre = genreRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> genreRepository.save(new Genre(name)));
            result.add(genre);
        }
        return result;
    }

    private Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Filme " + id + " não encontrado"));
    }
}
