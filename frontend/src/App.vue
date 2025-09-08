<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: "App",
};
</script>

<style>
body {
  margin: 0;
  font-family: 'Arial', sans-serif;
  background-color: #141414;
  color: #fff;
}

#app {
  padding: 20px;
}
a {
  text-decoration: none;
  color: inherit;
}
</style>
