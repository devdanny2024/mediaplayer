import { gql } from "@apollo/client/core";

export const GET_MOVIES = gql`
  query {
    movies {
      id
      title
      description
      thumbnailUrl
      videoUrls {
        quality
        url
      }
      subtitles {
        lang
        url
      }
    }
  }
`;
