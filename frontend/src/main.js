import { createApp, h, provide } from "vue";
import { DefaultApolloClient } from "@vue/apollo-composable";
import apolloClient from "./graphql/apolloClient";

import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";

const app = createApp({
  setup() {
    provide(DefaultApolloClient, apolloClient);
  },
  render: () => h(App),
});

app.use(router);
app.use(createPinia());
app.mount("#app");
