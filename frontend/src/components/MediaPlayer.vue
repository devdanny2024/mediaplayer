<template>
  <div class="player-overlay">
    <div
      class="player-container"
      @click="handleTap"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="handleTouchEnd"
    >
      <div class="seek-indicator" :class="{ visible: seekIndicatorVisible }">
        <img v-if="seekTimeOffset > 0" src="/icons/next.png" alt="Forward" />
        <img v-else src="/icons/back.png" alt="Rewind" />
        <span>{{ formattedSeekOffset }}</span>
      </div>

      <div v-if="loading" class="loading-spinner"></div>

      <video
        ref="video"
        class="video-player"
        autoplay
        playsinline
        @loadedmetadata="initializePlayer"
        @timeupdate="updateProgress"
        @volumechange="updateVolumeState"
        @play="handlePlay"
        @pause="() => (playing = false)"
        @waiting="loading = true"
        @playing="loading = false"
      >
        <source :src="selectedUrl" type="video/mp4" />
        Your browser does not support video.
      </video>

      <div class="player-ui" :class="{ 'controls-hidden': !controlsVisible }">
        <div v-if="!playing && !loading" class="center-play-pause">
          <img src="/icons/play-button.png" alt="Play" />
        </div>

        <button class="back-btn" @click.stop="$emit('close')">
          <img src="/icons/arrow.png" alt="Back" />
        </button>

        <div class="controls-wrapper" @click.stop>
          <div class="progress-bar-container" @click="seek">
            <div class="progress-bar">
              <div
                class="progress-filled"
                :style="{ width: progress + '%' }"
              ></div>
            </div>
          </div>
          <div class="controls">
            <div class="controls-left">
              <button class="control-btn" @click="togglePlay">
                <img
                  :src="playing ? '/icons/pause.png' : '/icons/play-button.png'"
                  alt="Play/Pause"
                />
              </button>
              <div class="volume-controls">
                <button class="control-btn" @click="toggleMute">
                  <img
                    v-if="isMuted || volume === 0"
                    src="/icons/silent.png"
                    alt="Muted"
                  />
                  <img
                    v-else-if="volume > 0.5"
                    src="/icons/volume.png"
                    alt="Volume High"
                  />
                  <img v-else src="/icons/volume-low.png" alt="Volume Low" />
                </button>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  class="volume-slider"
                  :value="volume"
                  @input="handleVolumeChange"
                />
              </div>
              <div class="time-display">
                {{ formattedCurrentTime }} / {{ formattedDuration }}
              </div>
            </div>

            <div class="controls-right">
              <button class="control-btn" @click="toggleSpeed">
                <span>{{ speeds[playbackRateIndex] }}x</span>
              </button>
              <button class="control-btn">
                <img src="/icons/settings.png" alt="Settings" />
              </button>
              <button class="control-btn" @click="toggleFullscreen">
                <img src="/icons/full-screen.png" alt="Fullscreen" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from "vue";

const props = defineProps({
  movie: Object,
});

const video = ref(null);
const playing = ref(false);
const fullscreen = ref(false);
const selectedUrl = ref(props.movie.videoUrls[0].url);
const speeds = [0.5, 1, 1.5, 2];
const playbackRateIndex = ref(1);
const volume = ref(1);
const isMuted = ref(true);
const currentTime = ref(0);
const duration = ref(0);
const progress = ref(0);
const controlsVisible = ref(true);
let inactivityTimer = null;
const isDragging = ref(false);
const wasPlayingBeforeDrag = ref(false);
const dragStartX = ref(0);
const dragStartTime = ref(0);
const seekTimeOffset = ref(0);
const seekIndicatorVisible = ref(false);
let seekIndicatorTimer = null;
const isKeyDown = ref(false);
let keyHoldTimer = null;
let holdSkipAmount = 15;
const loading = ref(true);

// Format Time
const formatTime = (time) => {
  if (isNaN(time)) return "00:00";
  const mins = Math.floor(time / 60);
  const secs = Math.floor(time % 60);
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
};
const formattedCurrentTime = computed(() => formatTime(currentTime.value));
const formattedDuration = computed(() => formatTime(duration.value));
const formattedSeekOffset = computed(() => {
  const sign = seekTimeOffset.value > 0 ? "+" : "";
  return `${sign}${Math.round(seekTimeOffset.value)}s`;
});

// Hide controls after inactivity
function hideControls() {
  if (playing.value && !isDragging.value) controlsVisible.value = false;
}
function handleUserActivity() {
  controlsVisible.value = true;
  clearTimeout(inactivityTimer);
  inactivityTimer = setTimeout(hideControls, 3000);
}

// Seek Indicator
function showSeekIndicator(offset) {
  seekTimeOffset.value = offset;
  seekIndicatorVisible.value = true;
  clearTimeout(seekIndicatorTimer);
  seekIndicatorTimer = setTimeout(() => {
    seekIndicatorVisible.value = false;
  }, 800);
}

// Controls
function togglePlay() {
  if (!video.value) return;
  video.value.paused ? video.value.play() : video.value.pause();
}
function skip(seconds) {
  if (!video.value) return;
  const newTime = video.value.currentTime + seconds;
  video.value.currentTime = Math.max(0, Math.min(duration.value, newTime));
  showSeekIndicator(seconds);
}
function toggleMute() {
  if (!video.value) return;
  video.value.muted = !video.value.muted;
}
function handleVolumeChange(event) {
  const newVolume = parseFloat(event.target.value);
  if (video.value) {
    video.value.volume = newVolume;
    video.value.muted = newVolume === 0;
  }
}
function toggleSpeed() {
  playbackRateIndex.value = (playbackRateIndex.value + 1) % speeds.length;
  if (video.value) video.value.playbackRate = speeds[playbackRateIndex.value];
}
function toggleFullscreen() {
  const playerContainer = document.querySelector(".player-overlay");
  if (!document.fullscreenElement) {
    playerContainer.requestFullscreen();
    fullscreen.value = true;
  } else {
    document.exitFullscreen();
    fullscreen.value = false;
  }
}
function seek(event) {
  if (isDragging.value || !video.value) return;
  const progressBar = event.currentTarget;
  const rect = progressBar.getBoundingClientRect();
  const percentage = (event.clientX - rect.left) / rect.width;
  video.value.currentTime = duration.value * percentage;
}

// Mobile Touch
function handleTap() {
  if (!isDragging.value) togglePlay();
}
function handleTouchStart(event) {
  wasPlayingBeforeDrag.value = playing.value;
  if (playing.value) video.value.pause();
  dragStartX.value = event.touches[0].clientX;
  dragStartTime.value = video.value.currentTime;
}
function handleTouchMove(event) {
  event.preventDefault();
  isDragging.value = true;
  if (!video.value) return;
  const deltaX = event.touches[0].clientX - dragStartX.value;
  const seekOffset = deltaX * 0.5;
  const newTime = Math.max(
    0,
    Math.min(duration.value, dragStartTime.value + seekOffset)
  );
  video.value.currentTime = newTime;
  showSeekIndicator(newTime - dragStartTime.value);
}
function handleTouchEnd() {
  if (wasPlayingBeforeDrag.value) video.value.play();
  setTimeout(() => (isDragging.value = false), 50);
}

// IMPROVED AUTO-LANDSCAPE FUNCTIONALITY
function handlePlay() {
  playing.value = true;
  loading.value = false;
  
  // Enhanced auto-landscape for mobile
  requestLandscapeMode();
}

// Auto-landscape functions
function requestLandscapeMode() {
  // Check if we're on a mobile device
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  
  if (!isMobile) return;
  
  try {
    // Method 1: Modern Screen Orientation API
    if (screen.orientation && screen.orientation.lock) {
      screen.orientation.lock('landscape-primary')
        .then(() => {
          console.log('Orientation locked to landscape');
        })
        .catch((error) => {
          console.log('Orientation lock failed:', error.message);
          // Fallback to other methods
          tryAlternateLandscapeMethods();
        });
    } 
    // Method 2: Older webkit prefix
    else if (screen.lockOrientation) {
      const result = screen.lockOrientation('landscape-primary') || 
                    screen.lockOrientation('landscape');
      if (!result) {
        tryAlternateLandscapeMethods();
      }
    }
    // Method 3: Even older webkit prefix
    else if (screen.webkitLockOrientation) {
      const result = screen.webkitLockOrientation('landscape-primary') || 
                    screen.webkitLockOrientation('landscape');
      if (!result) {
        tryAlternateLandscapeMethods();
      }
    }
    // Method 4: Mozilla prefix
    else if (screen.mozLockOrientation) {
      const result = screen.mozLockOrientation('landscape-primary') || 
                    screen.mozLockOrientation('landscape');
      if (!result) {
        tryAlternateLandscapeMethods();
      }
    }
    // Method 5: MS prefix
    else if (screen.msLockOrientation) {
      const result = screen.msLockOrientation('landscape-primary') || 
                    screen.msLockOrientation('landscape');
      if (!result) {
        tryAlternateLandscapeMethods();
      }
    }
    else {
      // No orientation API available
      tryAlternateLandscapeMethods();
    }
  } catch (error) {
    console.log('Orientation lock error:', error);
    tryAlternateLandscapeMethods();
  }
}

// Fallback methods when orientation lock doesn't work
function tryAlternateLandscapeMethods() {
  // Method 1: Prompt user to rotate device
  if (window.orientation !== undefined) {
    const currentOrientation = Math.abs(window.orientation);
    if (currentOrientation !== 90) {
      showRotationPrompt();
    }
  }
  
  // Method 2: CSS-based landscape optimization
  optimizeForLandscape();
}

// Show a prompt asking user to rotate device
function showRotationPrompt() {
  // Check if prompt already exists
  if (document.querySelector('.rotate-prompt')) return;
  
  const rotatePrompt = document.createElement('div');
  rotatePrompt.className = 'rotate-prompt';
  rotatePrompt.innerHTML = `
    <div class="rotate-content">
      <div class="rotate-icon">📱</div>
      <p>Please rotate your device to landscape for the best viewing experience</p>
    </div>
  `;
  
  document.body.appendChild(rotatePrompt);
  
  // Remove prompt when orientation changes
  const handleOrientationChange = () => {
    const currentOrientation = Math.abs(window.orientation || 0);
    if (currentOrientation === 90) {
      rotatePrompt.remove();
      window.removeEventListener('orientationchange', handleOrientationChange);
    }
  };
  
  window.addEventListener('orientationchange', handleOrientationChange);
  
  // Auto-remove prompt after 5 seconds
  setTimeout(() => {
    if (rotatePrompt.parentNode) {
      rotatePrompt.remove();
    }
    window.removeEventListener('orientationchange', handleOrientationChange);
  }, 5000);
}

// Optimize video player for landscape viewing
function optimizeForLandscape() {
  const playerContainer = document.querySelector('.player-container');
  if (playerContainer) {
    playerContainer.style.width = '100vw';
    playerContainer.style.height = '100vh';
  }
}

// Handle orientation changes
function handleOrientationChange() {
  setTimeout(() => {
    const currentOrientation = Math.abs(window.orientation || 0);
    if (currentOrientation === 90 && playing.value) {
      // Device is in landscape, optimize UI
      optimizeForLandscape();
    }
  }, 100); // Small delay to ensure orientation change is complete
}

// Keyboard Controls
function handleKeyDown(event) {
  if (event.target.tagName === "INPUT") return;
  handleUserActivity();
  switch (event.key.toLowerCase()) {
    case " ":
      event.preventDefault();
      togglePlay();
      break;
    case "m":
      event.preventDefault();
      toggleMute();
      break;
    case "f":
      event.preventDefault();
      toggleFullscreen();
      break;
    case "arrowright":
    case "arrowleft":
      event.preventDefault();
      if (isKeyDown.value) return;
      isKeyDown.value = true;
      const direction = event.key === "ArrowRight" ? 1 : -1;
      skip(holdSkipAmount * direction);
      keyHoldTimer = setTimeout(() => {
        const accelerate = () => {
          holdSkipAmount += 10;
          skip(holdSkipAmount * direction);
          keyHoldTimer = setTimeout(accelerate, 300);
        };
        accelerate();
      }, 500);
      break;
  }
}
function handleKeyUp(event) {
  if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
    isKeyDown.value = false;
    clearTimeout(keyHoldTimer);
    holdSkipAmount = 15;
    clearTimeout(seekIndicatorTimer);
    seekIndicatorTimer = setTimeout(() => {
      seekIndicatorVisible.value = false;
    }, 800);
  }
}

// Video Events
function updateProgress() {
  if (!video.value) return;
  currentTime.value = video.value.currentTime;
  progress.value = (currentTime.value / duration.value) * 100;
}
function updateVolumeState() {
  if (!video.value) return;
  isMuted.value = video.value.muted;
  volume.value = video.value.volume;
}
function initializePlayer() {
  if (!video.value) return;
  duration.value = video.value.duration;
  updateVolumeState();
  video.value.playbackRate = speeds[playbackRateIndex.value];
}

// Lifecycle
onMounted(() => {
  document.addEventListener("mousemove", handleUserActivity);
  window.addEventListener("keydown", handleKeyDown);
  window.addEventListener("keyup", handleKeyUp);
  // Listen for orientation changes
  window.addEventListener('orientationchange', handleOrientationChange);
  handleUserActivity();
});

onUnmounted(() => {
  clearTimeout(inactivityTimer);
  document.removeEventListener("mousemove", handleUserActivity);
  window.removeEventListener("keydown", handleKeyDown);
  window.removeEventListener("keyup", handleKeyUp);
  // Clean up orientation listener
  window.removeEventListener('orientationchange', handleOrientationChange);
});
</script>

<style>
.player-overlay {
  position: fixed;
  inset: 0;
  background: #000;
  z-index: 9999;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    "Helvetica Neue", Arial, sans-serif;
}
.player-container {
  position: relative;
  width: 100%;
  height: 100%;
  user-select: none;
  -webkit-user-select: none;
}
.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 1;
}
.player-ui {
  position: absolute;
  inset: 0;
  z-index: 10;
  opacity: 1;
  transition: opacity 0.4s ease-in-out;
}
.player-ui.controls-hidden {
  opacity: 0;
  pointer-events: none;
  cursor: none;
}

/* Loading Spinner */
.loading-spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 6px solid rgba(255, 255, 255, 0.3);
  border-top: 6px solid #ff6404;
  border-radius: 50%;
  width: 70px;
  height: 70px;
  animation: spin 1s linear infinite;
  z-index: 30;
}
@keyframes spin {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

/* Seek Indicator */
.seek-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 25;
  background-color: rgba(0, 0, 0, 0.6);
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 28px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}
.seek-indicator.visible {
  opacity: 1;
}
.seek-indicator img {
  width: 40px;
  height: 40px;
}
.center-play-pause {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  background-color: rgba(0, 0, 0, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  width: 100px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none;
}
.center-play-pause img {
  width: 60px;
  height: 60px;
  margin-left: 5px;
}

/* Buttons */
.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(0, 0, 0, 0.4);
  border: none;
  cursor: pointer;
  z-index: 20;
  padding: 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-btn img {
  width: 34px;
  height: 34px;
}
.control-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #fff;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.control-btn img {
  width: 36px;
  height: 36px;
  transition: transform 0.2s ease;
}
.control-btn span {
  font-size: 18px;
  font-weight: bold;
}
.control-btn:hover img,
.control-btn:hover span {
  transform: scale(1.2);
}

/* Controls Wrapper */
.controls-wrapper {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  padding: 0 20px 10px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
}
.controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.controls-left,
.controls-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* Progress & Time */
.progress-bar-container {
  width: 100%;
  padding: 12px 0;
  cursor: pointer;
}
.progress-bar {
  width: 100%;
  height: 6px;
  background-color: rgba(255, 255, 255, 0.3);
  position: relative;
  border-radius: 5px;
}
.progress-filled {
  height: 100%;
  background-color: #ff6404;
  border-radius: 5px;
  width: 0%;
}
.time-display {
  font-size: 15px;
  margin-left: 12px;
}

/* Volume Slider */
.volume-controls {
  display: flex;
  align-items: center;
}
.volume-slider {
  -webkit-appearance: none;
  width: 0;
  padding-left: 10px;
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  outline: none;
  transition: width 0.3s ease;
  cursor: pointer;
}
.volume-controls:hover .volume-slider {
  width: 100px;
}
.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
}
.volume-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
}

/* ROTATION PROMPT STYLES */
.rotate-prompt {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 10000;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.rotate-content {
  text-align: center;
  padding: 40px 20px;
  max-width: 300px;
}

.rotate-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: rotate-animation 2s linear infinite;
  display: inline-block;
}

.rotate-content p {
  font-size: 18px;
  line-height: 1.5;
  margin: 0;
}

@keyframes rotate-animation {
  0% { transform: rotate(0deg); }
  25% { transform: rotate(-15deg); }
  75% { transform: rotate(15deg); }
  100% { transform: rotate(0deg); }
}

/* LANDSCAPE OPTIMIZATIONS */
@media (orientation: landscape) and (max-width: 896px) {
  .player-overlay {
    width: 100vw;
    height: 100vh;
  }
  
  .video-player {
    width: 100vw;
    height: 100vh;
    object-fit: cover;
  }
  
  .controls-wrapper {
    padding: 0 40px 15px;
  }
  
  .back-btn {
    top: 15px;
    left: 15px;
  }
}

/* PORTRAIT MODE WARNING FOR MOBILE */
@media (orientation: portrait) and (max-width: 896px) {
  .player-overlay::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.3);
    pointer-events: none;
    z-index: 5;
  }
}
</style>