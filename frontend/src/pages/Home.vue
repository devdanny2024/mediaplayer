<template>
  <div>
    <h1 class="page-title">Movies</h1>
    <div class="movie-grid">
      <div
        v-for="movie in movies"
        :key="movie.id"
        class="movie-card"
        @click="selectMovie(movie)"
      >
        <img :src="movie.thumbnailUrl" :alt="movie.title" />
        <h3>{{ movie.title }}</h3>
      </div>
    </div>

    <!-- Render MediaPlayer when a movie is selected -->
    <MediaPlayer
      v-if="selectedMovie"
      :movie="selectedMovie"
      @close="selectedMovie = null"
    />
  </div>
</template>

<script setup>
import { ref } from "vue";
import { movies } from "../data/movies";
import MediaPlayer from "../components/MediaPlayer.vue";

const selectedMovie = ref(null);

function selectMovie(movie) {
  selectedMovie.value = movie; // this triggers MediaPlayer
}
</script>
