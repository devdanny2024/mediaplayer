package com.wanzami.backend.resolver;

import com.wanzami.backend.model.Movie;
import com.wanzami.backend.model.VideoQuality;
import com.wanzami.backend.model.Subtitle;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class MovieResolver {

    @QueryMapping
    public List<Movie> movies() {
        return List.of(
            new Movie(
                "1",
                "Inception",
                "A mind-bending thriller",
                "https://cdn.example.com/thumbnails/inception.jpg",
                List.of(
                    new VideoQuality("480p", "https://cdn.example.com/inception/480.m3u8"),
                    new VideoQuality("720p", "https://cdn.example.com/inception/720.m3u8"),
                    new VideoQuality("1080p", "https://cdn.example.com/inception/1080.m3u8")
                ),
                List.of(
                    new Subtitle("en", "https://cdn.example.com/inception/en.vtt"),
                    new Subtitle("fr", "https://cdn.example.com/inception/fr.vtt")
                )
            )
        );
    }

    @QueryMapping
    public Movie movie(@Argument String id) {
        return movies().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
