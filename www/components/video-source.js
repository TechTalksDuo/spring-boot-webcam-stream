import { LitElement, html, css } from "https://esm.run/lit/index.js";
import "https://esm.run/rvfc-polyfill";

import "./video-feedback.js";
import { getRandomColor } from "../utils/color.js";
import { WebSocketEvent, WebSocketEventType, WebSocketState } from "../events/websocket.js";
import { UserEvents, UserState } from "../events/user.js";

class VideoSource extends LitElement {
  #localVideoStream;
  #videoQuality = 0.7;
  #frameRate = 12;

  constructor() {
    super();

    UserState.addEventListener(UserEvents.update, () => {
      this.#videoQuality = 0.7 - Math.log10(Math.sqrt(Math.sqrt(UserState.users.length || 1)));
      this.requestUpdate();
    });

    WebSocketState.addEventListener(WebSocketEvent.open, () => {
      if (this.isVideoActive) {
        const video = this.shadowRoot.querySelector("video");
        this.#streamVideo(video, this.#localVideoStream);
      }
    });
  }

  static properties = {
    isVideoActive: { type: Boolean },
  };

  static styles = css`
    :host {
      position: relative;
      overflow: hidden;
    }

    :host(:not(:hover)) .streaming {
      display: none;
    }

    .controls {
      z-index: 1;
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);

      button {
        padding: clamp(0.5rem, 2.5vmin, 1rem);
        background-color: #17e;
        border-radius: 0.5rem;
        border: 0.25rem solid;
        color: #fff;
        font-size: clamp(0.75rem, 3vmin, 1.5rem);
        display: inline-flex;
        gap: 0.5rem;

        &.stop {
          background-color: #d24;
        }
      }
    }

    .username {
      position: absolute;
      left: 0;
      bottom: 0;

      &:empty {
        display: none;
      }

      &:not(:empty) {
        padding: clamp(0.25rem, 0.5vmin, 0.5rem) clamp(0.75rem, 1vmin, 1rem);
        background-color: #17e;
        color: #fff;
        border-radius: 0 0.5rem 0 0;
        z-index: 100;
        font-size: clamp(0.5rem, 3vmin, 1rem);
      }
    }

    video {
      object-fit: cover;
      background-color: var(--color, transparent);
      grid-area: video;
      width: 100%;
      height: 100%;
    }

    video-feedback {
      width: 100%;
      height: 100%;
      position: absolute;
      top: 0;
      left: 0;
    }

    .feather {
      width: clamp(0.75rem, 3vmin, 1.5rem);
      aspect-ratio: 1;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: round;
      stroke-linejoin: round;
      fill: none;
    }
  `;

  render() {
    return html`
      <video autoplay playsinline muted style=${`--color: ${getRandomColor()}`}></video>
      <video-feedback .username=${UserState.me?.username}></video-feedback>
      <span class="controls ${this.isVideoActive ? "streaming" : ""}">
        ${this.isVideoActive
          ? html`<button @click=${this.#stopVideo} class="stop">
              <svg class="feather">
                <use href="/assets/feather-sprite.svg#video-off" />
              </svg>
              Stop
            </button>`
          : html`<button @click=${this.#startVideo}>
              <svg class="feather">
                <use href="/assets/feather-sprite.svg#video" />
              </svg>
              Stream
            </button>`}
      </span>
      <span class="username">${this.#usernameOrInitials}</span>
    `;
  }

  async #startVideo() {
    const video = this.shadowRoot.querySelector("video");
    this.#localVideoStream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        width: { min: 128, ideal: 256 },
        height: { min: 128, ideal: 256 },
        frameRate: { ideal: this.#frameRate, max: this.#frameRate, min: this.#frameRate / 2 },
        facingMode: "user",
        resizeMode: "crop-and-scale",
      },
    });

    video.srcObject = this.#localVideoStream;
    this.isVideoActive = true;

    this.#streamVideo(video, this.#localVideoStream);
  }

  #stopVideo() {
    const video = this.shadowRoot.querySelector("video");
    this.#localVideoStream.getTracks().forEach((track) => track.stop());
    video.srcObject = null;
    this.isVideoActive = false;
    WebSocketState?.send({ type: WebSocketEventType.videoStopped });
  }

  #streamVideo(video, localVideoStream) {
    const snapshotCanvas = document.createElement("canvas").transferControlToOffscreen();
    const { width, height } = localVideoStream.getVideoTracks()[0].getSettings();
    snapshotCanvas.width = width;
    snapshotCanvas.height = height;

    video.requestVideoFrameCallback(() =>
      this.#processVideoFrame(video, snapshotCanvas, width, height)
    );
  }

  async #processVideoFrame(video, snapshotCanvas, width, height) {
    snapshotCanvas.getContext("2d").clearRect(0, 0, width, height);
    snapshotCanvas.getContext("2d").drawImage(video, 0, 0, width, height);

    const blob = await snapshotCanvas.convertToBlob({
      type: "image/jpeg",
      quality: this.#videoQuality,
    });
    const reader = new FileReader();
    reader.onload = () => WebSocketState?.send({ videoStream: reader.result });
    reader.readAsDataURL(blob);

    if (this.isVideoActive)
      video.requestVideoFrameCallback(() =>
        this.#processVideoFrame(video, snapshotCanvas, width, height)
      );
  }

  get #usernameOrInitials() {
    const { username } = UserState.me;
    if (!username) return "";
    return username.length * 2 < this.clientWidth
      ? username
      : username
          .split(" ")
          .map((n) => n[0])
          .join("");
  }
}

customElements.define("video-source", VideoSource);
