import { LitElement, html, css } from "https://esm.run/lit/index.js";

import "./video-feedback.js";
import { getRandomColor } from "../utils/color.js";
import { WebSocketState, WebSocketEvent, WebSocketEventType } from "../events/websocket.js";

class VideoTile extends LitElement {
  #imageWorker = new Worker("../image-worker.js");
  #imageWorkerListener = this.#updateImage.bind(this);

  constructor() {
    super();

    this.username = "";

    this.#imageWorker.addEventListener("message", this.#imageWorkerListener);

    WebSocketState.addEventListener(WebSocketEvent.message, ({ detail }) => {
      if (detail.type === WebSocketEventType.videoFromUser && detail.username === this.username) {
        this.isStreaming = true;
        this.#imageWorker.postMessage(detail.videoStream);
      }
      if (detail.type === WebSocketEventType.videoStopped && detail.username === this.username) {
        this.isStreaming = false;
        this.#clearIncomingImage();
        this.#imageWorker.removeEventListener("message", this.#imageWorkerListener);
      }
    });
  }

  static properties = {
    username: { type: String },
    isStreaming: { type: Boolean },
  };

  static styles = css`
    :host {
      background-color: var(--color, transparent);
      color: transparent;
      position: relative;
    }

    img {
      object-fit: cover;
      width: 100%;
      height: 100%;
      color: #fff;
    }

    video-feedback {
      width: 100%;
      height: 100%;
      position: absolute;
      top: 0;
      left: 0;
    }

    .no-image {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 50%;
      height: auto;
    }

    .username:not(:empty) {
      position: absolute;
      left: 0;
      bottom: 0;
      padding: clamp(0.25rem, 2.5vmin, 0.5rem) clamp(0.5rem, 2.5vmin, 1rem);
      background-color: #17e;
      color: #fff;
      border-radius: 0 0.5rem 0 0;
      z-index: 100;
      font-size: clamp(0.5rem, 3vmin, 1rem);
    }
  `;

  connectedCallback() {
    super.connectedCallback();
    this.style.setProperty("--color", getRandomColor());
  }

  disconnectedCallback() {
    super.disconnectedCallback();

    this.#imageWorker.terminate();
  }

  render() {
    return html`
      <img alt=${this.username} src="/assets/feather-sprite.svg#video-off" />
      ${!this.isStreaming
        ? html`<img class="no-image" alt="No image" src="/assets/user.svg" />`
        : ""}
      <video-feedback .username=${this.username}></video-feedback>
      <span class="username">${this.username}</span>
    `;
  }

  #clearIncomingImage() {
    const img = this.shadowRoot.querySelector("img");
    URL.revokeObjectURL(img.src);
    img.src = "/assets/feather-sprite.svg#video-off";
  }

  #updateImage(e) {
    const img = this.shadowRoot.querySelector("img");
    URL.revokeObjectURL(img.src);
    img.src = e.data;
  }
}

customElements.define("video-tile", VideoTile);
