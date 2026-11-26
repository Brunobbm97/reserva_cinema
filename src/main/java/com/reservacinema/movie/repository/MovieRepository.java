package com.reservacinema.movie.repository;

import com.reservacinema.movie.entity.Movie;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
