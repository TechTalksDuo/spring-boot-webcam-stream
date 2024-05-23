import { LitElement, html, css } from "https://esm.run/lit/index.js";

import { getRandomColor } from "../utils/color.js";
import { WebSocketState, WebSocketEvent, WebSocketEventType } from "../events/websocket.js";

class VideoTile extends LitElement {
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
      display: grid;
      grid-template-areas: "video";
    }

    img {
      object-fit: cover;
      width: 100%;
      height: 100%;
      grid-area: video;
      color: #fff;
    }

    .no-image {
      grid-area: video;
      justify-self: center;
      align-self: center;
      width: 50%;
      height: auto;
    }

    .username:not(:empty) {
      grid-area: video;
      justify-self: start;
      align-self: end;
      padding: 0.5rem 1rem;
      background-color: #17e;
      color: #fff;
      border-radius: 0 0.5rem 0 0;
      z-index: 1;
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
      <span class="username">${this.username}</span>
    `;
  }

  #showIncomingImage(videoStream) {
    const img = this.shadowRoot.querySelector("img");
    URL.revokeObjectURL(img.src);
    img.src = URL.createObjectURL(this.#dataURItoBlob(videoStream));
  }

  #clearIncomingImage() {
    const img = this.shadowRoot.querySelector("img");
    URL.revokeObjectURL(img.src);
    img.src = "/assets/feather-sprite.svg#video-off";
  }

  #dataURItoBlob(dataURI) {
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
