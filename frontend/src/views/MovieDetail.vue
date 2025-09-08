<template>
  <div v-if="movie" class="movie-detail">
    <h1>{{ movie.title }}</h1>
    <p>{{ movie.description }}</p>

    <video controls autoplay class="video-player">
      <source
        v-for="video in movie.videoUrls"
        :key="video.quality"
        :src="video.url"
        type="application/x-mpegURL"
      />
      Your browser does not support video.
    </video>

    <h3>Subtitles</h3>
    <ul>
      <li v-for="sub in movie.subtitles" :key="sub.lang">
        <a :href="sub.url" target="_blank">{{ sub.lang }}</a>
      </li>
    </ul>
  </div>
</template>

<script>
import { movies } from "../data/movies";

export default {
  props: ["id"],
  computed: {
    movie() {
      return movies.find((m) => m.id === this.id);
    },
  },
};
</script>

<style>
.movie-detail {
  max-width: 900px;
  margin: 0 auto;
}


ul {
  list-style: none;
  padding: 0;
}
li a {
  color: #1db954;
}
</style>
