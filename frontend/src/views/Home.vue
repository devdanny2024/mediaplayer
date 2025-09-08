<template>
  <div>
    <h1 class="page-title">Movies</h1>
    <div class="movie-grid">
      <div
        v-for="movie in movies"
        :key="movie.id"
        class="movie-card"
        @click="selectedMovie = movie"
      >
        <img :src="movie.thumbnailUrl" />
        <h3>{{ movie.title }}</h3>
      </div>
    </div>

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
</script>

<style>
.page-title { font-size: 2rem; margin-bottom: 20px; color: #fff; }
.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}
.movie-card {
  background-color: #1f1f1f;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s;
  cursor: pointer;
}
.movie-card img { width: 100%; display: block; }
.movie-card h3 { padding: 10px; text-align: center; font-size: 1rem; color: #fff; }
.movie-card:hover { transform: scale(1.05); }
</style>
