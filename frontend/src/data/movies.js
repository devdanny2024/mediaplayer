export const movies = [
  {
    id: "1",
    title: "Test Movie",
    description: "A short demo movie",
    thumbnailUrl: "/videos/test.jpg", // optional thumbnail you create
    videoUrls: [
      { quality: "720p", url: "/videos/output.mp4" }
    ],
    subtitles: [
      { lang: "en", url: "/videos/test.vtt" } // optional subtitle file
    ],
  },
];
