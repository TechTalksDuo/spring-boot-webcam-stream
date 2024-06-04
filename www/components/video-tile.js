import { LitElement, html, css } from "https://esm.run/lit/index.js";

import "./video-feedback.js";
import { getRandomColor } from "../utils/color.js";
import { WebSocketState, WebSocketEvent, WebSocketEventType } from "../events/websocket.js";

class VideoTile extends LitElement {
  #frameRate = 12;

  constructor() {
    super();

    this.username = "";
    WebSocketState.addEventListener(WebSocketEvent.message, ({ detail }) => {
      if (detail.type === WebSocketEventType.videoFromUser && detail.username === this.username) {
        this.isStreaming = true;
        this.#showIncomingImage(detail.videoStream);
      }
      if (detail.type === WebSocketEventType.videoStopped && detail.username === this.username) {
        this.isStreaming = false;
        this.#clearIncomingImage();
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

  #showIncomingImage(videoStream) {
    const interval = setInterval(() => {
      if (!videoStream.length || !this.isStreaming) clearInterval(interval);
      const img = this.shadowRoot.querySelector("img");
      const blob = this.#dataURItoBlob(videoStream.shift());
      if (blob) {
        URL.revokeObjectURL(img.src);
        img.src = URL.createObjectURL(blob);
      }
    }, 1000 / this.#frameRate);
    // const img = this.shadowRoot.querySelector("img");
    // URL.revokeObjectURL(img.src);
    // img.src = URL.createObjectURL(this.#dataURItoBlob(videoStream));
  }

  #clearIncomingImage() {
    const img = this.shadowRoot.querySelector("img");
    URL.revokeObjectURL(img.src);
    img.src = "/assets/feather-sprite.svg#video-off";
  }

  #dataURItoBlob(dataURI) {
    if (!dataURI) return null;
    const byteString = atob(dataURI.split(",")[1]);
    const mimeString = dataURI.split(",")[0].split(":")[1].split(";")[0];
    const arrayBuffer = new ArrayBuffer(byteString.length);
    const intArray = new Uint8Array(arrayBuffer);

    for (let i = 0; i < byteString.length; i++) {
      intArray[i] = byteString.charCodeAt(i);
    }

    return new Blob([arrayBuffer], { type: mimeString });
  }
}

customElements.define("video-tile", VideoTile);
